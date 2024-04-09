-----------------------
Creating a Codegen Repo
-----------------------

You'll want to create a repository for a Smithy code generator. Most Smithy
generators use Git repos hosted on GitHub. Smithy codegen repos are usually
titled ``smithy-<language>`` where ``<language>`` is the target programming
language. These repos contain:

1. Generic Smithy code generation, typically written in Java.
2. Runtime libraries used by the code generator.
3. Gradle build tooling to publish the code generator to places like
   `Maven Central`_. This is important as it allows others to use your code
   generator in their own projects.
4. Language-specific build tooling to build and publish the Smithy
   runtime libraries to language-specific artifact repositories (e.g.,
   Maven Central, NPM, RubyGems, crates.io, etc.).


Example codegen repositories
============================

Here are a few example Smithy codegen repos created by AWS:

- https://github.com/awslabs/smithy-typescript
- https://github.com/aws/smithy-go
- https://github.com/awslabs/smithy-rs
- https://github.com/awslabs/smithy-ruby
- https://github.com/awslabs/smithy-kotlin
- https://github.com/awslabs/smithy-swift


Codegen repo layout
===================

The root of a Smithy codegen repo should look and appear like a
typical repository for the target programming language. Code generation
should be isolated to a subdirectory named ``codegen`` that contains a
`multi-module Gradle package`_. A multi-module layout allows you to create
the code generator and an example package used to integration test the
generator. Java based repos will have a layout similar to the following:

.. code-block:: none

   .
   ├── CHANGES.md
   ├── CODE_OF_CONDUCT.md
   ├── CONTRIBUTING.md
   ├── LICENSE
   ├── NOTICE
   ├── README.md
   ├── codegen
   │   ├── README.md
   │   ├── build.gradle.kts
   │   ├── config
   │   │   ├── checkstyle
   │   │   │   ├── checkstyle.xml
   │   │   │   └── suppressions.xml
   │   │   └── spotbugs
   │   │       └── filter.xml
   │   ├── gradle
   │   │   └── wrapper
   │   │       ├── gradle-wrapper.jar
   │   │       └── gradle-wrapper.properties
   │   ├── gradle.properties
   │   ├── gradlew
   │   ├── gradlew.bat
   │   ├── settings.gradle.kts
   │   ├── smithy-mylang-codegen
   │   │   ├── build.gradle.kts
   │   │   └── src
   │   │       ├── main
   │   │       │   ├── java
   │   │       │   │   └── software
   │   │       │   │       └── amazon
   │   │       │   │           └── smithy
   │   │       │   │               └── mylang
   │   │       │   │                   └── codegen
   │   │       │   │                       └── MylangClientCodegenPlugin.java
   │   │       │   └── resources
   │   │       │       └── META-INF
   │   │       │           └── services
   │   │       │               └── software.amazon.smithy.build.SmithyBuildPlugin
   │   │       └── test
   │   │           ├── java
   │   │           │   └── software
   │   │           │       └── amazon
   │   │           │           └── smithy
   │   │           │               └── mylang
   │   │           │                   └── codegen
   │   │           │                       └── MylangClientCodegenPluginTest.java
   │   │           └── resources
   │   │               └── software
   │   │                   └── amazon
   │   │                       └── smithy
   │   │                           └── mylang
   │   │                               └── codegen
   │   └── smithy-mylang-codegen-test
   │       ├── build.gradle.kts
   │       ├── model
   │       │   ├── main.smithy
   │       └── smithy-build.json
   └── designs


Directory descriptions
----------------------

- ``codegen/``: All Smithy codegen functionality should appear in a
  sub-directory.
- ``codegen/smithy-mylang-codegen/``: Where the code generator is
  implemented in Java. Rename "mylang" to your generator's name. This
  project should eventually be published to Maven Central.
- ``codegen/smithy-mylang-codegen-test/``: A test project used to
  exercise the code generator. This project should not be published to
  Maven Central.
- ``designs/``: Public design documents. It's useful to publish design
  documents for the repo so consumers of the repo know how Smithy is
  mapped to the target environment and what tradeoffs were made in the
  implementation.


.. _codegen-creating-smithy-build-plugin:

Creating a Smithy-Build plugin
==============================

The entry point to any Smithy code generator is a Smithy-Build plugin
implementation of ``software.amazon.smithy.build.SmithyBuildPlugin``.
This plugin is discovered on the classpath and tells Smithy-Build what
plugin name it implements. For example, the simplest plugin looks
something like this:

.. code-block:: java

    package software.amazon.smithy.mylang.codegen;

    import software.amazon.smithy.build.PluginContext;
    import software.amazon.smithy.build.SmithyBuildPlugin;

    /**
     * Plugin to perform Mylang client code generation.
     */
    public final class MylangClientCodegenPlugin implements SmithyBuildPlugin {
        @Override
        public String getName() {
            // Tell Smithy-Build which plugin this is.
            return "mylang-client-codegen";
        }

        @Override
        public void execute(PluginContext context) {
            // Create and run the generator using the provided context.
            new MylangCodeGenerator(context).run();
        }
    }

Java is made aware of the plugin by adding the name of the plugin class
into a special META-INF file in:

.. code-block:: none

   codegen/smithy-mylang-codegen/src/main/resources/META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin

The file will contain a line that contains the full Java class name of
the plugin:

.. code-block:: none

   software.amazon.smithy.mylang.codegen.MylangClientCodegenPlugin

The next step is to implement the code generator.


Using Gradle
============

Smithy codegen projects typically use Gradle as a build tool for
compiling JARs, running JUnit tests, running Checkstyle, running
SpotBugs, and publishing JARs to Maven Central.


Running unit tests
------------------

Gradle by default looks for JUnit tests in
``codegen/smithy-mylang-codegen/src/test/java``. Tests are run using the
following command:

.. code-block:: none

   ./gradlew :smithy-mylang-codegen:test

(where ``:smithy-mylang-codegen`` is the module name to test and
``test`` is the target action to run).


Using Gradle with local packages
--------------------------------

When developing a Smithy code generator, you'll often need to work with
unreleased changes of the Smithy repo in other repos like an AWS SDK
code generator. If you use the Smithy codegen template repository, it
will automatically use whatever it finds in Maven Local, a local Maven
repository on your computer, rather than something like Maven Central, a
remote repository. You can add packages to Maven local using Gradle:

.. code-block:: none

    ./gradlew :smithy-mylang-codegen:pTML

If you need to use unreleased changes to
`awslabs/smithy <https://github.com/smithy-lang/smithy>`__, then clone the
repository and run:

.. code-block:: none

    ./gradlew pTML


FAQ
===

Do I have to use Gradle?
------------------------

No, you can use any build tool you'd like. All the Smithy codegen
implementations built by AWS as of January 2023 use Gradle to build their
generators, so it is likely the path of least resistance. Gradle has
plenty of usability issues, but it can do basically anything you'll
need, including publishing your generator to Maven Central. If you use
something other than Gradle, you might have extra work to do to create a
test project that generates code from a Smithy model.


Can I use Kotlin to do codegen?
-------------------------------

You can use any language you want to build a Smithy generator. If you're
building a Smithy code generator for an officially supported AWS SDK,
you are strongly encouraged to understand the business implications of
using Kotlin. Smithy's reference implementation is written in Java,
which a Kotlin code generator would use. However, building a Smithy code
generator in Java requires a team to learn and use Java. Using Kotlin
requires the team to learn Java *and* Kotlin.


I'm also building an AWS SDK. Where should that code go?
--------------------------------------------------------

There are various approaches you can take. The typical approach is to
have one GitHub repo dedicated to Smithy code generation and another
dedicated to the AWS SDK. Smithy is not AWS-specific and must be able
to generate code for teams outside of Amazon.

For branding and discoverability, official AWS SDKs should all be
available in GitHub repos dedicated to that SDK. This repository should
have a ``codegen`` module in a sub-directory that depends on and extends
the generic Smithy code generator for the language.


When should I publish codegen packages to Maven Central?
--------------------------------------------------------

Publish codegen packages to Maven Central just like any other software
project — when there are changes you want your consumers to use,
including the AWS SDK. AWS SDK code generators should also be published
to Maven Central to allow developers to generate code that uses
AWS signature version 4 or any AWS protocols.


.. _Maven Central: https://search.maven.org
.. _multi-module Gradle package: https://docs.gradle.org/current/userguide/multi_project_builds.html#multi_project_builds

==================
Generating Clients
==================

The Smithy Java :ref:`build plugin <plugins>`, ``java-client-codegen``, generates Java clients from Smithy models,
and can be executed with `Gradle <https://gradle.org/>`_ (recommended) or the :ref:`Smithy CLI <smithy-cli>`.

.. admonition:: Important
    :class: note

    The Smithy CLI is a prerequisite for this guide.
    See the :doc:`Smithy CLI installation guide <../../guides/smithy-cli/cli_installation>`
    if you do not already have the CLI installed.

-----------------------------------
Initial setup: Gradle (recommended)
-----------------------------------

To generate a Java client for a service, start by creating a new Smithy Gradle project.

The ``smithy init``` CLI command can be used to create a new Smithy Gradle project:

.. code-block:: sh

    smithy init -t quickstart-gradle

A Smithy Gradle project should contain a Gradle settings file, a Gradle build script,
a :ref:`smithy-build.json <smithy-build>` configuration file, and a `model/` directory
containing Smithy models. For example:

.. code-block:: sh

    my-project/
    ├── model/
    │   ├── ...
    ├── smithy-build.json
    ├── build.gradle.kts
    └── settings.gradle

Apply the `smithy-base`_ plugin to your Gradle build script to build your Smithy model
and execute build plugins:

.. code-block:: diff
   :caption: build.gradle.kts

    plugins {
        `java-library`
    +    id("software.amazon.smithy.gradle.smithy-base") version "__smithy_gradle_version__"
    }

Add the following dependencies to your project:

    1. Add the codegen `plugins`_ package as a ``smithyBuild`` dependency of your project. This makes the codegen
       plugins discoverable by the Smithy build task.
    2. Add the `client-core`_ package as a runtime dependency of your package.
       The  ``client-core``` package is the only required dependency for all generated clients.
    3. Add any additional dependencies used by your client, such as protocols or auth schemes.

.. code-block:: kotlin
   :caption: build.gradle.kts

    dependencies {
        // Add the code generation plugins to the smithy build classpath
        smithyBuild("software.amazon.smithy.java.codegen:plugins:__smithy_java_version__")

        // Add the client-core dependency needed by the generated code
        implementation("software.amazon.smithy.java:client-core:__smithy_java_version__")

        // Protocol implementations and auth schemes used by client
        implementation("com.example:my-protocol:1.0.0")
        implementation("com.example:my-auth-scheme:1.0.0")
    }

Now, define a service model in a ``model/`` directory at the root of your project.
The ``smithy-base``` Gradle plugin will automatically discover any models added to that directory.

---------------------------
Configuring code generation
---------------------------

In order to execute code generation, the ``java-client-codegen`` plugin must be added to
your :ref:`smithy-build <smithy-build>` config:

.. code-block:: diff
   :caption: smithy-build.json

    {
      "version": "1.0",
      "plugins": {
    +    "java-client-codegen": {
    +      "service": "com.example#CoffeeShop", // <- Replace with your service's ID
    +      // Generated Java code will use this as the root package namespace
    +      "namespace": "com.example.cafe"
    +    }
      }
    }

----------------------------------------
Add generated code to the Java sourceSet
----------------------------------------

Your package is now configured to generate Java client source code. However, the generated code must be
added to a `sourceSet <https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html>`_ to be
compiled by Gradle. To add the generated code to the ``main`` sourceSet, add the following to your
Gradle build script:

.. code-block:: kotlin
   :caption: build.gradle.kts

    // Add generated Java sources to the main sourceSet so they are compiled alongside
    // any other Java code in your package
    afterEvaluate {
        val clientPath = smithy.getPluginProjectionPath(smithy.sourceProjection.get(), "java-client-codegen")
        sourceSets {
            main {
                java {
                    srcDir(clientPath)
                }
            }
        }
    }

    // Ensure client files are generated before java compilation is executed.
    tasks.named("compileJava") {
        dependsOn("smithyBuild")
    }

---------------
Generating code
---------------

To generate and compile your client code, run a build from the root of your Gradle project:

.. code-block:: sh

    ./gradlew clean build

Building the project will generate code in the
``build/smithy-projections/<project-name>/source/java-client-codegen/`` directory.

----------------
Complete example
----------------

The following Gradle build script and ``smithy-build.json`` files provide a complete example of how to configure a
Gradle project to generate a Smithy Java client:

.. code-block:: kotlin
   :caption: build.gradle.kts

    plugins {
        `java-library`
        id("software.amazon.smithy.gradle.smithy-base") version "__smithy_gradle_version__"
    }

    dependencies {
        // Add the code generation plugin to the smithy build dependencies
        smithyBuild("software.amazon.smithy.java.codegen:client:__smithy_java_version__")

        // Add any smithy model dependencies as `implementation` dependencies here.
        // For example, you might add additional trait packages here.
        implementation("...")

        // Add the client-core dependency needed by the generated code
        implementation("software.amazon.smithy.java:client-core:__smithy_java_version__"")

        // Also add your protocol implementations or auth schemes as dependencies
        implementation("com.example:my-protocol:1.0.0")
        implementation("com.example:my-auth-scheme:1.0.0")
    }

    // Add generated Java sources to the main sourceSet so they are compiled alongside
    // any other java code in your package
    afterEvaluate {
        val clientPath = smithy.getPluginProjectionPath(smithy.sourceProjection.get(), "java-client-codegen")
        sourceSets {
            main {
                java {
                    srcDir(clientPath)
                }
            }
        }
    }

    // Ensure client files are generated before java compilation is executed.
    tasks.named("compileJava") {
        dependsOn("smithyBuild")
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

.. code-block:: json
   :caption: smithy-build.json

    {
      "version": "1.0",
      "plugins": {
        "java-client-codegen": {
          "service": "com.example#CoffeeShop",
          "namespace": "com.example.cafe",
          // Default protocol for the client. Must have a corresponding trait in the
          // model and implementation discoverable via SPI (see section on protocols below)
          "protocol": "aws.protocols#restJson1",
          // Adds a common header to all generated files
          "headerFile": "license.txt"
        }
      }
    }


.. _smithy-base: https://github.com/smithy-lang/smithy-gradle-plugin#smithy-base-plugin
.. _client-core: https://mvnrepository.com/artifact/software.amazon.smithy.java/client-core
.. _plugins: https://mvnrepository.com/artifact/software.amazon.smithy.java.codegen/plugins

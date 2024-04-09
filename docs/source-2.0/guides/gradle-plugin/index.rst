.. _smithy-gradle-plugin:

=====================
Smithy Gradle Plugins
=====================

The `Smithy Gradle plugins`_ integrate Smithy with the `Gradle`_ build system. These plugin can
build artifacts from Smithy models, and generate JARs for Smithy models and model
:ref:`projections <projections>`.

.. toctree::
    :maxdepth: 1
    :caption: Migrate to version 0.10.0+

    gradle-migration-guide

Plugins
=======

Two official `Gradle`_ plugins are available:

* `smithy-base`_:
    This plugin configures the `SourceSet`_'s and `Configuration`_'s
    for a Smithy project. It also creates the base ``smithyBuild`` task for the project that
    builds the Smithy models and build artifacts for the project.
* `smithy-jar`_:
    Adds Smithy files to an existing JAR created by a ``jar`` task (usually created
    by the `Java plugin`_ or `Kotlin JVM plugin`_). The ``smithy-jar`` plugin also adds build metadata
    and tags to the JAR's MANIFEST. The ``smithy-jar`` plugin applies the ``smithy-base`` plugin when
    applied.

.. admonition:: Which plugin should I use?
    :class: note

    Most users should use the ``smithy-jar`` plugin. If you are building shared model packages,
    custom traits, custom linters, or codegen integrations you should use the ``smithy-jar`` plugin.
    If you are writing a code generator you can use the ``smithy-base`` plugin to set up basic
    `Configuration`_'s and provide access to Smithy gradle tasks.

.. _plugin-apply:

Applying Plugins
----------------

The Smithy Gradle plugins are applied using the ``plugins`` block of a gradle build script.
The following example configures a project to use the ``smithy-base`` Gradle plugin:

.. tab:: Kotlin

    .. code-block:: kotlin
        :caption: build.gradle.kts

        plugins {
            id("software.amazon.smithy.gradle.smithy-base").version("__smithy_gradle_version__")
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        plugins {
            id 'software.amazon.smithy.gradle.smithy-base' version '__smithy_gradle_version__'
        }


.. _smithy-model-sources:

.. note::

    The ``smithy-jar`` plugin requires that a `jar` task be registered
    before it is applied. The `Java Plugin`_ and `Kotlin JVM plugin`_
    are commonly used plugins that both register a `jar` task.

Complete Examples
=================

For several complete examples, see the `examples directory`_ of the `Smithy
Gradle plugins`_ repository, or check out the :doc:`Quick start guide </quickstart>` for a tutorial on
creating a Smithy model and building it with the Smithy Gradle plugin.

The examples in from the `examples directory`_  can be copied into your workspace using
the :ref:`Smithy CLI <smithy-cli>` ``init`` command as follows:

.. code-block:: text

    smithy init -t <EXAMPLE_NAME> -o <OUTPUT_DIRECTORY> --url https://github.com/smithy-lang/smithy-gradle-plugin

You can list all examples available in the `Smithy Gradle plugins`_ repository with the following command:

.. code-block:: text

    smithy init --list --url https://github.com/smithy-lang/smithy-gradle-plugin

.. _building-smithy-models:

Building Smithy models
======================

The ``smithyBuild`` task that builds smithy models operates in two different modes:

1. If no ``projection`` is specified for the ``SmithyExtension``, then the task
   runs a "source" build using the "source" projection.
2. If a ``projection`` is specified for the ``SmithyExtension``, then the task
   runs in "projection" mode.

.. _building-source-model:

Building a source model
-----------------------

A "source" build is run when no ``sourceProjection`` is configured in
``SmithyExtension``. To prevent accidentally relying on Smithy models
that are only available as build dependencies, Smithy models are discovered
using only the :ref:`model sources <model-sources>` and ``runtimeClasspath``.

The following example ``build.gradle.kts`` will build a Smithy model using a
"source" build:

.. tab:: Kotlin

    .. code-block:: kotlin
        :caption: build.gradle.kts

        plugins {
            `java-library`
            id("software.amazon.smithy.gradle.smithy-jar").version("__smithy_gradle_version__")
        }

        // The SmithyExtension is used to customize the build. This example
        // doesn't set any values and can be completely omitted.
        smithy {}

        repositories {
            mavenLocal()
            mavenCentral()
        }

        dependencies {
            implementation("software.amazon.smithy:smithy-model:__smithy_version__")

            // These are just examples of dependencies. This model has a dependency on
            // a "common" model package and uses the external AWS traits.
            implementation("com.foo.baz:foo-model-internal-common:1.0.0")
            implementation("software.amazon.smithy:smithy-aws-traits:__smithy_version__")
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        plugins {
            id 'java-library'
            'software.amazon.smithy.gradle.smithy-jar' version '__smithy_gradle_version__'
        }

        // The SmithyExtension is used to customize the build. This example
        // doesn't set any values and can be completely omitted.
        smithy {}

        repositories {
            mavenLocal()
            mavenCentral()
        }

        dependencies {
            implementation 'software.amazon.smithy:smithy-model:__smithy_version__'

            // These are just examples of dependencies. This model has a dependency on
            // a "common" model package and uses the external AWS traits.
            implementation 'com.foo.baz:foo-model-internal-common:1.0.0'
            implementation 'software.amazon.smithy:smithy-aws-traits:__smithy_version__'
        }

.. _generating-projection:

Generating a projection
-----------------------

A "projection" build is run when a :ref:`projection <projections>` is
specified in the ``SmithyExtension``. You might create a projection of
a model if you need to maintain an internal version of a model that contains
more information and features than an external version of a model published to
your customers.

Any projected models should be added to the ``smithyBuild`` configuration. This
prevents packages with projected models from appearing as dependencies of the
projected version of the model.

The following example gradle build script will run a "projection"
build using the "external" projection.

.. tab:: Kotlin

        .. code-block:: kotlin
            :caption: build.gradle.kts

            plugins {
                `java-library`
                id("software.amazon.smithy.gradle.smithy-jar").version("__smithy_gradle_version__")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation("software.amazon.smithy:smithy-aws-traits:__smithy_version__")

                // Take a dependency on the internal model package. This
                // dependency *must* be a smithyBuild dependency to ensure
                // that is does not appear in the generated JAR.
                smithyBuild("com.foo.baz:foo-internal-model:1.0.0")
            }

            smithy {
                // Use the "external" projection. This projection must be found in the
                // smithy-build.json file. This also ensures that models found in the
                // foo-internal-package that weren't filtered out are copied into the
                // projection created by this package.
                sourceProjection.set("external")
                projectionSourceTags.addAll("com.foo.baz:foo-internal-model")
            }

.. tab:: Groovy

        .. code-block:: groovy
            :caption: build.gradle

            plugins {
                id 'java-library'
                id 'software.amazon.smithy.gradle.smithy-jar' version '__smithy_gradle_version__'
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation 'software.amazon.smithy:smithy-aws-traits:__smithy_version__'

                // Take a dependency on the internal model package. This
                // dependency *must* be a smithyBuild dependency to ensure
                // that is does not appear in the generated JAR.
                smithyBuild 'com.foo.baz:foo-internal-model:1.0.0'
            }

            smithy {
                // Use the "external" projection. This projection must be found in the
                // smithy-build.json file. This also ensures that models found in the
                // foo-internal-package that weren't filtered out are copied into the
                // projection created by this package.
                sourceProjection = "external"
                projectionSourceTags += ["com.foo.baz:foo-internal-model"]
            }

Because the ``sourceProjection`` of the ``SmithyExtension`` was set to ``external``, a
``smithy-build.json`` file **must** be found that defines the ``external``
projection. For example:

.. code-block:: json
    :caption: smithy-build.json

    {
        "version": "1.0",
        "projections": {
            "external": {
                "transforms": [
                    {
                        "name": "excludeShapesByTag",
                        "args": {
                            "tags": ["internal"]
                        }
                    },
                    {
                        "name": "excludeShapesByTrait",
                        "args": {
                            "traits": ["internal"]
                        }
                    },
                    {
                        "name": "excludeMetadata",
                        "args": {
                            "keys": ["suppressions", "validators"]
                        }
                    },
                    {
                        "name": "removeUnusedShapes"
                    }
                ]
            }
        }
    }

.. _projection-tags:

Projection tags
^^^^^^^^^^^^^^^

Projections can *project* and filter shapes from dependencies into the output model.
"Projecting" a dependency into your output JAR will include that model in the output
sources of your project. Downstream consumers of your JAR will then no longer need to
include the "projected" dependency as a dependency of their project to resolve the
"projected" shapes. You need to specify which dependencies are being projected into
your JAR by setting the ``projectionSourceTags`` property.

.. tab:: Kotlin

        .. code-block:: kotlin
            :caption: build.gradle.kts

            smithy {
                sourceProjection.set("external")
                projectionSourceTags.addAll("Foo", "com.baz:bar")
            }

.. tab:: Groovy

        .. code-block:: groovy
            :caption: build.gradle

            smithy {
                projection = "external"
                projectionSourceTags += setOf("Foo", "com.baz:bar")
            }


Tags are used to logically group packages to make it easier to build
projections. The ``tags`` property is used to add ``Smithy-Tags`` attribute
to the JAR MANIFEST.

.. tab:: Kotlin

        .. code-block:: kotlin
            :caption: build.gradle.kts

            smithy {
                tags.addAll("X", "foobaz-model")
            }

.. tab:: Groovy

        .. code-block:: groovy
            :caption: build.gradle

            smithy {
                tags += ["X", "foobaz-model"]
            }


For example, if your service is made up of 10 packages, you can add the
"foobaz-model" Smithy tag to each package so that the only value that needs
to be provided for ``tags`` to correctly project your model is "foobaz-model".

When building a model package, the Smithy Gradle plugin will automatically add
the group name of the package being built, the group name + ":" + name of the package,
and group name + ":" + name + ":" version. This allows models to always
be queried by group and artifact names in addition to custom tags.

.. _artifacts-from-smithy-models:

Building artifacts from Smithy models
-------------------------------------

If a :ref:`smithy-build.json <smithy-build>` file is found at the root of the project,
then it will be used to generate :ref:`artifacts <projection-artifacts>` from the Smithy
model. Smithy uses :ref:`build plugins <plugins>` to generate projection artifacts. To
use a smithy build plugin with Gradle, first add it as a ``smithyBuild`` dependency in
the Gradle build script. The plugin will then be discoverable and can be configured
in the ``smithy-build.json`` file.

For example, to generate a Typescript client from a Smithy model, add the
``smithy-typescript-codegen`` package as a ``smithyBuild`` dependency:

.. tab:: Kotlin

        .. code-block:: kotlin
            :caption: build.gradle.kts

            dependencies {
                // ...

                // This dependency is required in order to apply the "typescript-client-codegen"
                // plugin in smithy-build.json
                smithyBuild("software.amazon.smithy.typescript:smithy-typescript-codegen:__smithy_typescript_version__")
            }

.. tab:: Groovy

        .. code-block:: groovy
            :caption: build.gradle

            dependencies {
                // ...

                // This dependency is required in order to apply the "typescript-client-codegen"
                // plugin in smithy-build.json
                smithyBuild 'software.amazon.smithy.typescript:smithy-typescript-codegen:__smithy_typescript_version__'
            }

The plugin can then be configured in the ``smithy-build.json`` to generate a typescript client
from a Smithy model:

.. code-block:: json
    :caption: smithy-build.json

    {
        "version": "1.0",
        "plugins": {
            "typescript-client-codegen": {
                "package": "@smithy/typescript-client-codegen-example",
                "packageVersion": "0.0.1",
                "packageJson": {
                    "license": "MIT"
                }
            }
        }
    }

.. _gradle-plugin-configuration:

Configuration
=============

.. _model-sources:

Smithy model sources
--------------------

Smithy gradle plugins assume Smithy model files (``*.smithy``) are organized in a similar way as Java source files,
in sourceSets. The smithy-base plugin adds a new sources block named smithy to every sourceSet.
By default, this source block will include Smithy models in:

* ``model/``
* ``src/$sourceSetName/smithy``
* ``src/$sourceSetName/resources/META-INF/smithy``

New source directories can be added to a smithy sources block as follows:

.. tab:: Kotlin

    .. code-block:: kotlin
        :caption: build.gradle.kts

        sourceSets {
            main {
                smithy {
                    srcDir("includes")
                }
            }
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        sourceSets {
            main {
                smithy {
                    srcDir 'includes'
                }
            }
        }

Models found in these directories are combined into a flattened directory
structure and used to validate and build the Smithy model. A Smithy manifest
file is automatically created for the detected models, and it, along with the
model files, are placed in the ``META-INF/smithy/`` resource of the created
JAR. Any project that depends on this created JAR will be able to find
and use the Smithy models contained in the JAR when using *model discovery*.

Dependencies
------------

The Smithy build plugins use two different configurations to search for dependencies
such as shared models or :ref:`smithy build plugins <plugins>`:

* ``runtimeClasspath``:
    Runtime dependencies that will be required by any output JARS or publications.
    For example, a shared model package or Java trait definition.
* ``smithyBuild``:
    Build dependencies that are not required by output models.

Runtime dependencies can be added directly to the ``runtimeClasspath`` configuration
or to a configuration that extends ``runtimeClasspath``, such as the ``implementation``
configuration added by the ``java-library`` plugin.

.. _dependencies:

Build Dependencies
^^^^^^^^^^^^^^^^^^

The ``smithy-base`` plugin adds a ``smithyBuild`` `Configuration`_ that can be used to specify
dependencies used when calling ``smithy build``. These dependencies will not be included in any output JARs or publications. They are akin to `compileOnly`
dependencies for Java projects. Smithy build plugins and projected dependencies should be
included in the `smithyBuild` configuration. For example:

.. tab:: Kotlin

    .. code-block:: kotlin
        :caption: build.gradle.kts

        dependencies {
            smithyBuild("com.example.software:build-only:1.0.0")
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        dependencies {
            smithyBuild 'com.example.software:build-only:1.0.0'
        }

.. _smithy-extension-properties:

Smithy extension properties
---------------------------

The Smithy Gradle plugins are configured using the `SmithyExtension`_ extension:

.. tab:: Kotlin

    .. code-block:: kotlin
        :caption: build.gradle.kts

        smithy {
            projection.set("foo")
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        smithy {
            projection = "foo"
        }


This extension supports the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - format
      - ``boolean``
      - Flag indicating whether to format Smithy source files. By default, the :ref:`Smithy CLI <smithy-cli>` ``format``
        command is executed on all source directories. This opinionated formatter follows the best practices
        recommended by the Smithy team. It is possible to disable the formatter by setting this flag to ``false``.
    * - smithyBuildConfigs
      - ``FileCollection``
      - Sets a custom collection of smithy-build.json files to use when building the model.
    * - sourceProjection
      - ``String``
      - Sets the :ref:`projection <projections>` name to use as the source (primary) projection.
        The smithy sources for this projection will be packaged in output JARs by the ``smithy-jar``
        plugin. There must be a corresponding projection definition in the :ref:`smithy-build.json <smithy-build-json>`
        file in the project. Defaults to ``"source"``.
    * - projectionSourceTags
      - ``Set<String>``
      - Get the tags that are searched for in classpaths when determining
        which models are projected into the created JAR. This plugin will look
        through the JARs in the buildscript classpath to see if they contain a
        META-INF/MANIFEST.MF attribute named "Smithy-Tags" that matches any of
        the given projection source tags. The Smithy models found in each
        matching JAR are copied into the JAR being projected. This allows a
        projection JAR to aggregate models into a single JAR.
    * - tags
      - ``Set<String>``
      - Set the tags that are added to the JAR. These tags are placed in the
        META-INF/MANIFEST.MF attribute named "Smithy-Tags" as a comma
        separated list. JARs with Smithy-Tags can be queried when building
        projections so that the Smithy models found in each matching JAR are
        placed into the projection JAR.
    * - allowUnknownTraits
      - ``boolean``
      - Sets whether or not unknown traits in the model should be ignored. By
        default, the build will fail if unknown traits are encountered.
    * - fork
      - ``boolean``
      - By default, the CLI is run in the same process as Gradle,
        but inside a thread with a custom class loader. This should
        work in most cases, but there is an option to run inside a
        process if necessary.
    * - outputDirectory
      - ``Directory``
      - Defines where Smithy build artifacts are written.


Customize output directory
^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default, Smithy build artifacts will be placed in the project build
directory in a ``smithyprojections/`` directory. There are two ways to override
the output directory. The first method is to set the ``outputDirectory`` property
in the ``smithy-build.json`` config for your Smithy project. For example:

.. code-block:: json
    :caption: smithy-build.json

    {
        "outputDirectory": "build/output"
    }

The second method is to configure the plugin using the ``smithy`` extension:

.. tab:: Kotlin

    .. code-block:: kotlin
        :caption: build.gradle.kts

        smithy {
            outputDirectory.set(file("path/to/output"))
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        smithy {
            outputDirectory = file("path/to/output")
        }

.. note::

    Setting the output directory on the plugin extension will
    override any ``outputDirectory`` property set in the
    smithy-build config.

Set ``smithy-build`` configs to use
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default, the plugin will look for a file called ``smithy-build.json``
at the project's root and will use that as the :ref:`smithy-build config <smithy-build>`
for your project. If no smithy-build.json file is found, then an empty build config
is used to build the project.

Alternatively, you can explicitly configure one or more smithy-build configs to use for
your project as follows:

.. tab:: Kotlin

    .. code-block:: kotlin
        :caption: build.gradle.kts

        smithy {
            smithyBuildConfigs.set(files("smithy-build-config.json"))
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        smithy {
            smithyBuildConfigs = files("smithy-build-config.json")
        }

Set Smithy Tags to add to a JAR
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When the smithy-jar plugin is applied to a project, it can add a number of Smithy
tags to the MANIFEST of a generated JAR. These tags can be used by downstream
consumers to filter which models to include in projections. Tags can be configured
for the plugin as follows:

.. tab:: Kotlin

    .. code-block:: kotlin
        :caption: build.gradle.kts

        smithy {
            tags.addAll("tag1", "anotherTag", "anotherTag2")
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        smithy {
            tags += ["tag1", "anotherTag", "anotherTag2"]
        }

Fork a new process when executing Smithy CLI commands
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default, :ref:`Smithy CLI <smithy-cli>` commands are run in the
same process as Gradle, but inside a thread with a custom class loader.
This should work in most cases, but there is an option to run inside a
process if necessary. To run Smithy CLI commands in a process set the
``fork`` configuration option to true:

.. tab:: Kotlin

    .. code-block:: kotlin
        :caption: build.gradle.kts

        smithy {
            fork.set(true)
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        smithy {
            fork = true
        }

.. _disable-smithy-formatter:

Disable Smithy Formatter
^^^^^^^^^^^^^^^^^^^^^^^^

By default, the :ref:`Smithy CLI <smithy-cli>` ``format`` command is
executed on all source directories. This opinionated formatter follows
the best practices recommended by the Smithy team. It is possible to disable
the formatter by setting the format setting on the plugin extension to false:

.. tab:: Kotlin

    .. code-block::
        :caption: build.gradle.kts

        smithy {
            format.set(false)
        }

.. tab:: Groovy

    .. code-block:: groovy
        :caption: build.gradle

        smithy {
            format = false
        }

.. _examples directory: https://github.com/awslabs/smithy-gradle-plugin/tree/main/examples
.. _Smithy Gradle plugins: https://github.com/awslabs/smithy-gradle-plugin/
.. _Gradle: https://gradle.org/
.. _smithy-base: https://github.com/smithy-lang/smithy-gradle-plugin#smithy-base-plugin
.. _smithy-jar: https://github.com/smithy-lang/smithy-gradle-plugin#smithy-jar-plugin
.. _Configuration: https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html
.. _SourceSet: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html
.. _Java plugin: https://docs.gradle.org/current/userguide/java_plugin.html
.. _Kotlin JVM plugin: https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
.. _SmithyExtension: https://github.com/smithy-lang/smithy-gradle-plugin/blob/main/smithy-base/src/main/java/software/amazon/smithy/gradle/SmithyExtension.java

.. _smithy-gradle-plugin:

====================
Smithy Gradle Plugin
====================

The `Smithy Gradle plugin`_ integrates Smithy with `Gradle`_. This plugin can
build artifacts from Smithy models, generate JARs that contain Smithy models
found in Java projects, and generate JARs that contain filtered :ref:`projections <projections>`
of Smithy models.


.. _plugin-install:

Installation
============

The Smithy Gradle plugin is applied using the ``software.amazon.smithy`` plugin.
The following example configures a project to use the Smithy Gradle plugin:

.. code-block:: kotlin

    plugins {
        id("software.amazon.smithy").version("0.6.0")
    }


.. _smithy-model-sources:

Smithy model sources
====================

When a JAR is generated for a project, any Smithy models found in the
following directories will be added to the JAR:

* ``model/``
* ``src/main/smithy``
* ``src/main/resources/META-INF/smithy``

Models found in these directories are combined into a flattened directory
structure and used to validate and build the Smithy model. A Smithy manifest
file is automatically created for the detected models, and it along with the
model files, are placed in the ``META-INF/smithy/`` resource of the created
JAR. Any project that then depends on this created JAR will be able to find
and use the Smithy models contained in the JAR when using *model discovery*.


.. _building-smithy-models:

Building Smithy models
======================

This plugin operates in two different modes:

1. If no ``projection`` is specified for the ``SmithyExtension``, then the plugin
   runs a "source" build using the "source" projection.
2. If a ``projection`` is specified for the ``SmithyExtension``, then the plugin
   runs in a "projection" mode.


.. _smithy-extension-properties:

Smithy extension properties
===========================

This plugin is configured using the ``software.amazon.smithy.gradle.SmithyExtension``
extension:

.. code-block:: kotlin

    configure<software.amazon.smithy.gradle.SmithyExtension> {
        projection = "foo"
    }


This extension supports the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - projection
      - ``String``
      - Sets the :ref:`projection <projections>` name to build. There must
        be a corresponding projection definition in the :ref:`smithy-build.json <smithy-build-json>`
        file in the project.
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
    * - smithyBuildConfigs
      - ``FileCollection``
      - Sets a custom collection of smithy-build.json files to use when
        building the model.
    * - allowUnknownTraits
      - ``boolean``
      - Sets whether or not unknown traits in the model should be ignored. By
        default, the build will fail if unknown traits are encountered.
    * - outputDirectory
      - ``File``
      - Defines where Smithy build artifacts are written.


.. _building-source-model:

Building a source model
-----------------------

A "source" build is run when no ``projection`` is configured in
``SmithyExtension``. Because no projection was specified, **smithy-build** is
executed using the ``compileClasspath`` plus the ``buildscript`` classpath. To
prevent accidentally relying on Smithy models that are only available to
build scripts, Smithy models are discovered using only the
``compileClasspath`` and ``runtimeClasspath``.

The following example ``build.gradle.kts`` will build a Smithy model using a
"source" build:

.. code-block:: kotlin

    plugins {
        id("software.amazon.smithy").version("0.6.0")
    }

    // The SmithyExtension is used to customize the build. This example
    // doesn't set any values and can be completely omitted.
    configure<software.amazon.smithy.gradle.SmithyExtension> {}

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


.. _generating-projection:

Generating a projection
-----------------------

A "projection" build is run when a :ref:`projection <projections>` is
specified in the ``SmithyExtension``. You might want to create a projection of
a model if you need to maintain an internal version of a model that contains
more information and features than an external version of a model published to
your customers.

A "projection" build is executed using only the ``buildscript`` classpath, and
Smithy models are discovered using only the ``buildscript`` classpath. This
prevents models discovered in the original model from appearing in the
projected version of the model.

The following example ``build.gradle.kts`` file will run a "projection"
build that uses the "external" projection.

.. code-block:: kotlin

    plugins {
        id("software.amazon.smithy").version("0.6.0")
    }

    buildscript {
        repositories {
            mavenLocal()
            mavenCentral()
        }
        dependencies {
            classpath("software.amazon.smithy:smithy-aws-traits:__smithy_version__")

            // Take a dependency on the internal model package. This
            // dependency *must* be a buildscript only dependency to ensure
            // that is does not appear in the generated JAR.
            classpath("com.foo.baz:foo-internal-model:1.0.0")
        }
    }

    // Use the "external" projection. This projection must be found in the
    // smithy-build.json file. This also ensures that models found in the
    // foo-internal-package that weren't filtered out are copied into the
    // projection created by this package.
    configure<software.amazon.smithy.gradle.SmithyExtension> {
        projection = "external"
        projectionSourceTags = setOf("com.foo.baz:foo-internal-model")
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        implementation("software.amazon.smithy:smithy-model:__smithy_version__")

        // Any dependencies that the projected model needs must be (re)declared
        // here. For example, let's assume that the smithy-aws-traits package is
        // needed in the projected model too.
        implementation("software.amazon.smithy:smithy-aws-traits:__smithy_version__")
    }


Because the ``projection`` of the ``SmithyExtension`` was set to ``external``, a
``smithy-build.json`` file **must** be found that defines the ``external``
projection. For example:

.. code-block:: json

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
---------------

Projections are meant to *project* and filter other models into another
model. You need to specify which dependencies are being projected into your
JAR by setting the ``projectionSourceTags`` property.

.. code-block:: kotlin

    configure<software.amazon.smithy.gradle.SmithyExtension> {
            projection = "external"
            projectionSourceTags = setOf("Foo", "com.baz:bar")
    }


Tags are used to logically group packages to make it easier to build
projections. The ``tags`` property is used to add ``Smithy-Tags`` to a JAR.

.. code-block:: kotlin

    configure<software.amazon.smithy.gradle.SmithyExtension> {
        tags = setOf("X", "foobaz-model")
    }


For example, if your service is made up of 10 packages, you can add the
"foobaz-model" Smithy tag to each package so that the only value that needs
to be provided for ``tags`` to correctly project your model is "foobaz-model".

When building a model package, this plugin will automatically add the group
name of the package being built, the group name + ":" + name of the package,
and group name + ":" + name + ":" version. This allows models to always
be queried by group and artifact names in addition to custom tags.


.. _artifacts-from-smithy-models:

Building artifacts from Smithy models
-------------------------------------

If a ``smithy-build.json`` file is found at the root of the project, then it
will be used to generate :ref:`artifacts <projection-artifacts>` from the Smithy model.

The following example generates an OpenAPI model from a Smithy model:

.. code-block:: json

    {
        "version": "1.0",
        "plugins": {
            "openapi": {
                "service": "foo.baz#MyService"
            }
        }
    }


The above Smithy plugin also requires a ``buildscript`` dependency in
``build.gradle.kts``:

.. code-block:: kotlin

    buildscript {
        // ...
        dependencies {
            // ...

            // This dependency is required in order to apply the "openapi"
            // plugin in smithy-build.json
            classpath("software.amazon.smithy:smithy-openapi:__smithy_version__")
        }
    }

Complete Examples
=================

For several complete examples, see the `examples directory`_ of the Smithy
Gradle plugin repository, or check out the :doc:`Quick start guide </quickstart>` for a tutorial on
creating a Smithy model and building it with the Smithy Gradle plugin.

.. _examples directory: https://github.com/awslabs/smithy-gradle-plugin/tree/main/examples
.. _Smithy Gradle plugin: https://github.com/awslabs/smithy-gradle-plugin/
.. _Gradle: https://gradle.org/

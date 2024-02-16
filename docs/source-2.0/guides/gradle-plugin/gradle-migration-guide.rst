.. _gradle_migration_guide:

==========================================
Migrating to Gradle plugin version 0.10.0+
==========================================

This guide describes how to migrate your Gradle build files
to use the `Smithy Gradle plugins`_ v0.10.0+ without breaking
your existing Gradle projects.

.. warning::
    Versions 0.10.0+ of the Smithy Gradle plugins will automatically apply
    the Smithy formatter to your Smithy source files. This can result in a
    large number of formatting changes the first time you apply these plugins
    to your project. For instructions on how to disable automatic formatting
    see :ref:`Disabling the Smithy formatter <disable-smithy-formatter>`.

Update plugin artifact
======================

The Smithy Gradle plugin has been broken up into separate capability
(``smithy-jar``) and convention (``smithy-base``) plugins. The
``smithy-jar`` plugin operates similarly to the previous ``smithy`` plugin,
but no longer applies the ``java`` plugin automatically. To migrate to the
new plugin, change the artifact id in the ``plugins`` block and apply the
``java`` (or ``java-library``) plugin if it is not already applied to
your project.

.. tab:: Kotlin

    .. code-block:: diff
        :caption: build.gradle.kts

        plugins {
        -     id("software.amazon.smithy").version("0.7.0")
        +     `java`
        +     id("software.amazon.smithy.gradle.smithy-jar").version("0.10.0")
        }

.. tab:: Groovy

    .. code-block:: diff
        :caption: build.gradle

        plugins {
        -      id 'software.amazon.smithy' version '0.7.0'
        +      id 'java'
        +      id 'software.amazon.smithy.gradle.smithy-jar' version '0.10.0'
        }

Remove Buildscript Dependencies
===============================

Pre-0.10.0 versions of the Smithy Gradle plugins use the buildscript block
for build-only dependencies such as Smithy build plugins. The 0.10.0+
Gradle plugins create a new Gradle `Configuration`_ ``smithyBuild``
for these build-only dependencies.

.. tab:: Kotlin

    .. code-block:: diff
        :caption: build.gradle.kts

        -buildscript {
        -       repositories {
        -           mavenLocal()
        -           mavenCentral()
        -       }
        -
        -       dependencies {
        -           // Dependency repeated here and below because model discovery previously
        -           // only ran on buildscript classpath for projections
        -           classpath("software.amazon.smithy:smithy-aws-traits:__smithy_version__")
        -           // Take a dependency on the internal model package. This
        -           // dependency *must* be a buildscript only dependency to ensure
        -           // that is does not appear in the generated JAR.
        -           classpath("com.foo.baz:foo-internal-model:1.0.0")
        -       }
        -}

        dependencies {
            implementation("software.amazon.smithy:smithy-model:__smithy_version__")

            implementation("software.amazon.smithy:smithy-aws-traits:__smithy_version__")
        +   smithyBuild("com.foo.baz:foo-internal-model:1.0.0")
        }

.. tab:: Groovy

    .. code-block:: diff
        :caption: build.gradle

        -buildscript {
        -       repositories {
        -           mavenLocal()
        -           mavenCentral()
        -       }
        -
        -       dependencies {
        -           // Dependency repeated here and below because model discovery previously
        -           // only ran on buildscript classpath for projections
        -           classpath 'software.amazon.smithy:smithy-aws-traits:__smithy_version__'
        -           // Take a dependency on the internal model package. This
        -           // dependency *must* be a buildscript only dependency to ensure
        -           // that is does not appear in the generated JAR.
        -           classpath 'com.foo.baz:foo-internal-model:1.0.0'
        -       }
        -}

        dependencies {
            implementation 'software.amazon.smithy:smithy-model:__smithy_version__'

            implementation 'software.amazon.smithy:smithy-aws-traits:__smithy_version__'
        +   smithyBuild 'com.foo.baz:foo-internal-model:1.0.0'
        }

Change ``projection`` property name
===================================

The property ``projection`` has also been updated to ``sourceProjection``.

.. tab:: Kotlin

        .. code-block:: diff
            :caption: build.gradle.kts

            -configure<software.amazon.smithy.gradle.SmithyExtension> {
            +smithy {
            -    projection = "foo"
            +    sourceProjection.set("foo")
            }

.. tab:: Groovy

        .. code-block:: diff
            :caption: build.gradle

            -configure<software.amazon.smithy.gradle.SmithyExtension> {
            +smithy {
            -    projection = "foo"
            +    sourceProjection = "foo"
            }

Change ``smithyBuildJar`` task name
===================================

The ``smithyBuildJar`` task has been removed. Instead, the plugin now
executes separate ``smithyBuild`` and ``smithyJarStaging`` tasks. If
your project previously configured the ``smithyBuildJar``,
configure the ``smithyBuild`` task instead. Tasks that depended on
``smithyBuildJar`` should now depend on the ``jar`` task.

.. tab:: Kotlin

        .. code-block:: diff
            :caption: build.gradle.kts

            tasks {
            -   smithyBuildJar {
            +   smithyBuild {
                    smithyBuildConfigs.set(files("smithy-build.json", other))
                }
                // ..
            }

            -tasks["smithyBuildJar"].dependsOn("otherTask")
            +tasks["jar"].dependsOn("otherTask")

.. tab:: Groovy

        .. code-block:: diff
            :caption: build.gradle

            tasks {
            -   smithyBuildJar {
            +   smithyBuild {
                    smithyBuildConfigs = files("smithy-build.json", other)
                }
                // ..
            }

            -tasks["smithyBuildJar"].dependsOn("otherTask")
            +tasks["jar"].dependsOn("otherTask")

.. _Smithy Gradle plugins: https://github.com/awslabs/smithy-gradle-plugin/
.. _Configuration: https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html

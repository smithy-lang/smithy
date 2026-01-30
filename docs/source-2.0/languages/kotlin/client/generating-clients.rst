==================
Generating Clients
==================

The Smithy Kotlin :ref:`build plugin <plugins>`, ``kotlin-codegen``, generates Kotlin clients from Smithy models,
and can be executed with `Gradle <https://gradle.org/>`_ (recommended) or the :ref:`Smithy CLI <smithy-cli>`.

.. admonition:: Important
    :class: note

    The Smithy CLI is a prerequisite for this guide.
    See the :doc:`Smithy CLI installation guide </guides/smithy-cli/cli_installation>`
    if you do not already have the CLI installed.

-----------------------------------
Initial setup: Gradle (recommended)
-----------------------------------

To generate a Kotlin client for a service, start by creating a new Smithy Gradle project.

The ``smithy init`` CLI command can be used to create a new Smithy Gradle project:

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
    └── settings.gradle.kts

To your Gradle build script, apply the the `Kotlin JVM <https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm>`_ plugin,
and the `smithy-base <https://github.com/smithy-lang/smithy-gradle-plugin#smithy-base-plugin>`_ plugin (for building your Smithy model):

.. code-block:: diff
   :caption: build.gradle.kts

   plugins {
   +    kotlin("jvm") version "2.3.0"
   +    id("software.amazon.smithy.gradle.smithy-base") version "1.3.0"
   }

Add a `version catalog <https://docs.gradle.org/current/userguide/version_catalogs.html>`_ to manage your dependencies and their versions:

.. code-block:: toml
    :caption: gradle/libs.versions.toml

    smithy-kotlin-version="1.6.0"
    coroutines-core-version="1.10.2"

    smithy-kotlin-codegen = { module = "aws.smithy.kotlin:codegen", version.ref = "smithy-kotlin-version" }

    smithy-kotlin-runtime-core = { module = "aws.smithy.kotlin:runtime-core", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-smithy-client = { module = "aws.smithy.kotlin:smithy-client", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-http-client = { module = "aws.smithy.kotlin:http-client", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-telemetry-api = { module = "aws.smithy.kotlin:telemetry-api", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-telemetry-defaults = { module = "aws.smithy.kotlin:telemetry-defaults", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-rpcv2-protocol = { module = "aws.smithy.kotlin:smithy-rpcv2-protocols", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-aws-protocol-core = { module = "aws.smithy.kotlin:aws-protocol-core", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-aws-signing-common = { module = "aws.smithy.kotlin:aws-signing-common", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-serde = { module = "aws.smithy.kotlin:serde", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-serde-cbor = { module = "aws.smithy.kotlin:serde-cbor", version.ref = "smithy-kotlin-version" }
    smithy-kotlin-http-client-engine-default = { module = "aws.smithy.kotlin:http-client-engine-default", version.ref = "smithy-kotlin-version" }

    kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines-core-version" }

Add the following dependencies to your project:

.. code-block:: kotlin
   :caption: build.gradle.kts

    dependencies {
        // Code generator
        compileOnly(libs.smithy.kotlin.codegen)

        // Client Dependencies
        implementation(libs.smithy.kotlin.runtime.core)
        implementation(libs.smithy.kotlin.smithy.client)
        implementation(libs.smithy.kotlin.http.client)
        implementation(libs.smithy.kotlin.telemetry.api)
        implementation(libs.smithy.kotlin.telemetry.defaults)
        implementation(libs.smithy.kotlin.rpcv2.protocol)
        implementation(libs.smithy.kotlin.aws.protocol.core)
        implementation(libs.smithy.kotlin.aws.signing.common)
        implementation(libs.smithy.kotlin.serde)
        implementation(libs.smithy.kotlin.serde.cbor)
        implementation(libs.smithy.kotlin.http.client.engine.default)
        implementation(libs.kotlinx.coroutines.core)
    }

Now, define a service model in a ``model/`` directory at the root of your project.
The ``smithy-base`` Gradle plugin will automatically discover any models added to that directory.

---------------------------
Configuring code generation
---------------------------

In order to execute code generation, the ``kotlin-codegen`` plugin must be added to
your :ref:`smithy-build <smithy-build>` config:

.. code-block:: diff
   :caption: smithy-build.json

   {
     "version": "1.0",
     "plugins": {
   +     "kotlin-codegen": {
   +       "service": "com.example#CoffeeShop", // Replace with your service's ID
   +       "sdkId": "CoffeeShop", // Replace with your service's SDK ID
   +       "package": {
   +         "name": "io.smithy.kotlin.client.example", // Generated Kotlin code will use this as the root package namespace
   +         "version": "0.0.1"
   +       }
   +     }
     }
   }

------------------------------------
Add Smithy build to the Kotlin build
------------------------------------

To ensure your client code is generated on every Kotlin build, Gradle must be configured to run a Smithy build before
Kotlin compilation in your Gradle build script:

.. code-block:: kotlin
    :caption: build.gradle.kts

    tasks.named("compileKotlin") {
        dependsOn("smithyBuild")
    }

------------------------------------------
Add generated code to the Kotlin sourceSet
------------------------------------------

Your package is now configured to generate Kotlin client source code. However, the generated code must be added to a
`source set <https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html>`_ to be compiled.
To add the generated code to the main source set, add the following to your Gradle build script:

.. code-block:: kotlin
    :caption: build.gradle.kts

    afterEvaluate {
        val clientPath = smithy.getPluginProjectionPath(smithy.sourceProjection.get(), "kotlin-codegen")
        sourceSets.main.get().kotlin.srcDir(clientPath)
    }


-----------------------
Opt in to internal APIs
-----------------------

Some of the code generated client APIs are public but marked with an `InternalApi <https://github.com/smithy-lang/smithy-kotlin/blob/main/runtime/runtime-core/common/src/aws/smithy/kotlin/runtime/Annotations.kt#L8-L31>`_
annotation to discourage client end users from using them outside of generated code.
To opt in to the InternalApi annotation, add the following to your Gradle build script:

.. code-block:: kotlin
    :caption: build.gradle.kts

    val optinAnnotations = listOf("kotlin.RequiresOptIn", "aws.smithy.kotlin.runtime.InternalApi")
    kotlin.sourceSets.all {
        optinAnnotations.forEach { languageSettings.optIn(it) }
    }

---------------
Generating code
---------------

To generate and compile your client code, run a build from the root of your Gradle project:

.. code-block:: sh

    ./gradlew clean build

Building the project will generate code in the
``build/smithy-projections/<project-name>/source/kotlin-codegen/`` directory.

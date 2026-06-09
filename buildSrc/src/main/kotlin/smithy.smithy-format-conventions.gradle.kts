/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

// Runs the locally-built smithy-cli to format this project's `.smithy` files.
//
// We don't use the smithy-jar plugin's bundled smithyFormat task because its task
// graph drags in smithyBuild, which needs :smithy-cli on its classpath. Many
// consumers here are themselves dependencies of :smithy-cli, so that wiring forms
// a Gradle task cycle. A standalone JavaExec task avoids the cycle.

// We use a private configuration name (rather than `smithyCli`) because the smithy-jar
// plugin already creates one with that name and would clash if both plugins are applied.
val smithyFormatCli: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    description = "Resolves the locally-built smithy CLI used to format Smithy IDL files."
}

dependencies {
    smithyFormatCli(project(path = ":smithy-cli", configuration = "shadow"))
}

// Default discovery roots. Source models live either in `src/main` (jar-resource layout)
// or a top-level `model/` directory (smithy-jar plugin's default model directory). We
// keep each root as a separately-anchored FileTree so Gradle's implicit-dependency
// validation sees narrow output locations, not the whole project root.
val smithyFormatSources: FileCollection = files(
    fileTree("src/main") { include("**/*.smithy") },
    fileTree("model") { include("**/*.smithy") },
)

// When `-PsmithyFormatCheck` is set, the task passes `--check` to the CLI: it fails
// instead of writing changes. Useful for CI to assert that committed files are formatted.
val checkOnly = project.hasProperty("smithyFormatCheck")

val smithyFormat = tasks.register<JavaExec>("smithyFormat") {
    group = "formatting"
    description = "Formats Smithy IDL files in this project."
    classpath = smithyFormatCli
    mainClass.set("software.amazon.smithy.cli.SmithyCli")
    val sources = smithyFormatSources
    inputs.files(sources).withPathSensitivity(PathSensitivity.RELATIVE).skipWhenEmpty()

    // The format task mutates .smithy files in place, so the same files are both input
    // and output. Declaring them as `outputs.files(sources)` is the natural thing to do,
    // but it makes Gradle flag every other task that reads them (compileJava,
    // processResources, ...) as implicitly depending on us, which Gradle 9 promotes from
    // a warning to a failure. Wiring dependsOn(smithyFormat) onto each consumer would
    // satisfy the validator but creates a cycle: this task pulls in :smithy-cli's shadow
    // jar, which transitively needs :smithy-model:processResources, which would then
    // depend back on :smithy-model:smithyFormat.
    //
    // Instead we declare no outputs and tell Gradle "trust me, you're up to date as long
    // as inputs haven't changed". The trade-off is losing automatic ordering -- other
    // tasks no longer implicitly know format must run first -- which we compensate for
    // with the explicit `build dependsOn smithyFormat` wiring below.
    outputs.upToDateWhen { true }
    doFirst {
        val cliArgs = mutableListOf("format")
        if (checkOnly) {
            cliArgs.add("--check")
        }
        cliArgs.addAll(sources.files.map { it.absolutePath })
        args = cliArgs
        if (sources.files.isEmpty()) {
            // JavaExec fails on empty args list -- short-circuit.
            throw StopExecutionException("No .smithy files to format.")
        }
    }
}

tasks.named("build") { dependsOn(smithyFormat) }

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

import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option

abstract class SmithyFormatTask : JavaExec() {

    private var check: Boolean = false

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val sources: ConfigurableFileCollection

    @Option(option = "check", description = "Fail if files need formatting instead of rewriting them.")
    fun setCheck(check: Boolean) {
        this.check = check
    }

    @Internal
    fun getCheck(): Boolean = check

    init {
        group = "formatting"
        description = "Formats Smithy IDL files in this project."
        mainClass.set("software.amazon.smithy.cli.SmithyCli")

        // The format task mutates .smithy files in place, so the same files are both
        // input and output. Declaring outputs makes Gradle flag every task that reads
        // them as implicitly depending on us, which Gradle 9 promotes to a failure.
        // Wiring dependsOn onto each consumer would satisfy the validator but creates a
        // cycle through :smithy-cli. Instead we declare no outputs and rely on inputs
        // for up-to-date checking and the explicit `build dependsOn` wiring below.
        outputs.upToDateWhen { true }
    }

    override fun exec() {
        val cliArgs = mutableListOf("format")
        if (check) {
            cliArgs.add("--check")
        }
        cliArgs.addAll(sources.files.map { it.absolutePath })
        args = cliArgs
        super.exec()
    }
}

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

tasks.register<SmithyFormatTask>("smithyFormat") {
    classpath = smithyFormatCli

    // Default discovery roots. Source models live either in `src/main` (jar-resource layout)
    // or a top-level `model/` directory (smithy-jar plugin's default model directory). We
    // keep each root as a separately-anchored FileTree so Gradle's implicit-dependency
    // keep each root as a separately-anchored FileTree so Gradle's implicit-dependency
    // validation sees narrow output locations, not the whole project root.
    sources.from(
        fileTree("src/main") { include("**/*.smithy") },
        fileTree("model") { include("**/*.smithy") },
    )
}

tasks.named("build") { dependsOn("smithyFormat") }

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import org.apache.tools.ant.taskdefs.condition.Os
import org.beryx.runtime.RuntimeTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active
import org.jreleaser.model.Distribution
import org.jreleaser.model.Stereotype

plugins {
    application
    alias(libs.plugins.jreleaser) apply false
    alias(libs.plugins.runtime)
    alias(libs.plugins.shadow)
    id("smithy.module-conventions")
    id("smithy.integ-test-conventions")
}

description = "This module implements the Smithy command line interface."

extra["displayName"] = "Smithy :: CLI"
extra["moduleName"] = "software.amazon.smithy.cli"

val imageJreVersion = "17"
val correttoRoot = "https://corretto.aws/downloads/latest/amazon-corretto-$imageJreVersion"

dependencies {
    // Keeps these as exported transitive dependencies.
    implementation(project(":smithy-model"))
    implementation(project(":smithy-build"))
    implementation(project(":smithy-diff"))
    implementation(project(":smithy-syntax", "shadow"))

    // This is needed to ensure the above dependencies are added to the runtime image.
    shadow(project(":smithy-model"))
    shadow(project(":smithy-build"))
    shadow(project(":smithy-diff"))
    shadow(project(":smithy-syntax"))

    // These maven resolver dependencies are shaded into the smithy-cli JAR.
    implementation(libs.maven.resolver.provider)
    implementation(libs.maven.resolver.api)
    implementation(libs.maven.resolver.spi)
    implementation(libs.maven.resolver.util)
    implementation(libs.maven.resolver.impl)
    implementation(libs.maven.resolver.connector.basic)
    implementation(libs.maven.resolver.transport.file)
    implementation(libs.maven.resolver.transport.http)
    implementation(libs.slf4j.jul) // Route slf4j used by Maven through JUL like the rest of Smithy.

    testImplementation(libs.mockserver)
}

// ------ Setup CLI binary -------
// This setting is needed by the Shadow plugin for some reason to define a main application class.
val mainClassName = "software.amazon.smithy.cli.SmithyCli"
application {
    mainClass = mainClassName
    applicationName = "smithy"
}

// Detect which OS and arch is running to create an application class data sharing
// archive for the current platform. This is not how we'll ultimately build and release images.
var imageOs = ""
if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    imageOs = "windows-x64"
} else if (Os.isFamily(Os.FAMILY_MAC)) {
    if (Os.isArch("aarch64")) {
        imageOs = "darwin-aarch64"
    } else if (Os.isArch("x86_64") || Os.isArch("amd64")) {
        imageOs = "darwin-x86_64"
    } else {
        println("No JDK for ${System.getProperty("os.arch")}")
    }
} else if (Os.isFamily(Os.FAMILY_UNIX)) {
    if (Os.isArch("aarch")) {
        imageOs = "linux-aarch64"
    } else if (Os.isArch("x86_64") || Os.isArch("amd64")) {
        imageOs = "linux-x86_64"
    } else {
        println("No JDK for ${System.getProperty("os.arch")}")
    }
} else {
    println("Unknown OS and arch: ${System.getProperty("os.name")}")
}

// This is needed in order for integration tests to find the CLI runtime image
val smithyBinary =
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        layout.buildDirectory.file("image/smithy-cli-$imageOs/bin/smithy.bat").get().asFile.path
    } else {
        layout.buildDirectory.file("image/smithy-cli-$imageOs/bin/smithy").get().asFile.path
    }
System.setProperty("SMITHY_BINARY", smithyBinary)

runtime {
    addOptions("--compress", "2", "--strip-debug", "--no-header-files", "--no-man-pages")
    addModules("java.logging", "java.xml", "java.naming", "jdk.crypto.ec")

    launcher {
        // This script is a combination of the default startup script used by the badass runtime
        // plugin, and the upstream source it's based on:
        // https://raw.githubusercontent.com/gradle/gradle/master/subprojects/plugins/src/main/resources/org/gradle/api/internal/plugins/unixStartScript.txt
        // Using the Gradle wrapper script as-is results in a huge startup penalty, so I instead updated parts of the
        // script that didn't affect performance, and kept others that did. Namely, the set and eval code of the Gradle
        // startup script is significantly slower than what was used by the plugin.
        unixScriptTemplate = file("configuration/unixStartScript.txt")
        jvmArgs =
            listOf(
                // Disable this when attempting to profile the CLI. In 99% of use cases this isn't not necessary.
                "-XX:-UsePerfData",
                "-Xshare:auto",
                "-XX:SharedArchiveFile={{BIN_DIR}}/../lib/smithy.jsa",
            )
    }

    targetPlatform("linux-x86_64") {
        setJdkHome(jdkDownload("$correttoRoot-x64-linux-jdk.tar.gz"))
    }

    targetPlatform("linux-aarch64") {
        setJdkHome(jdkDownload("$correttoRoot-aarch64-linux-jdk.tar.gz"))
    }

    targetPlatform("darwin-x86_64") {
        setJdkHome(jdkDownload("$correttoRoot-x64-macos-jdk.tar.gz"))
    }

    targetPlatform("darwin-aarch64") {
        setJdkHome(jdkDownload("$correttoRoot-aarch64-macos-jdk.tar.gz"))
    }

    targetPlatform("windows-x64") {
        setJdkHome(jdkDownload("$correttoRoot-x64-windows-jdk.zip"))
    }

    // Because we're using target-platforms, it will use this property as a prefix for each target zip
    imageZip = layout.buildDirectory.file("image/smithy-cli.zip")

    // This is needed to ensure that jlink is available (jlink is Java 9+), we should use the JDK that
    // is configured for the runtime
    // NOTE: For the runtime task, you *must* have the right JDK set up in your environment (17 at the time of writing)
    javaHome =
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(imageJreVersion))
        }.map { it.metadata.installationPath.asFile.path }
}

tasks {

    val cliVersion by registering(Copy::class) {
        from(configurations.runtimeClasspath).include("*-all.jar")
        into("src/main/resources")
    }

    register("generateVersionFile") {
        val versionFile =
            sourceSets.main.map { sourceSet ->
                sourceSet.output.resourcesDir?.resolve("software/amazon/smithy/cli/cli-version")
                    ?: throw GradleException("Resources directory not found for main sourceSet")
            }

        outputs.file(versionFile)

        doLast {
            versionFile.get().writeText(project.version.toString())
        }
    }

    processResources {
        dependsOn("generateVersionFile")
    }

    shadowJar {
        // Replace the normal JAR with the shaded JAR. We don't want to publish a JAR that isn't shaded.
        archiveClassifier.set("")

        mergeServiceFiles()

        // Shade dependencies to prevent conflicts with other dependencies.
        relocate("org.slf4j", "software.amazon.smithy.cli.shaded.slf4j")
        relocate("org.eclipse", "software.amazon.smithy.cli.shaded.eclipse")
        relocate("org.apache", "software.amazon.smithy.cli.shaded.apache")
        relocate("org.sonatype", "software.amazon.smithy.cli.shaded.sonatype")
        relocate("org.codehaus", "software.amazon.smithy.cli.shaded.codehaus")

        // If other javax packages are ever pulled in, we'll need to update this list. This is more deliberate about
        // what's shaded to ensure that things like javax.net.ssl.SSLSocketFactory are not inadvertently shaded.
        relocate("javax.annotation", "software.amazon.smithy.cli.shaded.javax.annotation")
        relocate("javax.inject", "software.amazon.smithy.cli.shaded.javax.inject")

        // Don't shade Smithy dependencies into the CLI. These are normal dependencies that we want our consumers
        // to resolve.
        dependencies {
            exclude(project(":smithy-utils"))
            exclude(project(":smithy-model"))
            exclude(project(":smithy-build"))
            exclude(project(":smithy-diff"))
            exclude(project(":smithy-syntax"))
        }
    }

    val runtime by getting(RuntimeTask::class) {
        dependsOn(shadowJar)

        // Add finishing touches to the distributables, such as an install script, before it gets zipped
        doLast {
            targetPlatforms.get().forEach { targetPlatform ->
                copy {
                    from("configuration")
                    include(if (targetPlatform.value.name.contains("windows")) "install.bat" else "install")
                    into(layout.buildDirectory.dir("image/smithy-cli-${targetPlatform.value.name}"))
                }
            }
        }
    }

    // Run the warmup command after the image is generated
    val optimize by registering(Exec::class) {
        commandLine(smithyBinary, "warmup")
        dependsOn(runtime)
    }

    // Always shadow the JAR and replace the JAR by the shadowed JAR.
    jar {
        finalizedBy(shadowJar)
    }

    // Prevent https://docs.gradle.org/7.3.3/userguide/validation_problems.html#implicit_dependency issues between
    // the runtime image and shadowJar tasks.
    distZip {
        dependsOn(shadowJar)
    }

    distTar {
        dependsOn(shadowJar)
    }

    startScripts {
        dependsOn(shadowJar)
    }

    // ------ Setup integration testing -------
    integ {
        systemProperty("SMITHY_BINARY", smithyBinary)
        // Configuration parameters to execute top-level classes in parallel but methods in same thread
        systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
        systemProperties["junit.jupiter.execution.parallel.mode.default"] = "same_thread"
        systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"

        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
        }

        // Runtime images need to be created before integration tests can run.
        dependsOn(runtime)
    }
}

// ------ Setup Jreleaser -------
if (project.hasProperty("release.cli")) {
    apply(plugin = "org.jreleaser")

    tasks.named("assembleDist") {
        doFirst {
            // This is a workaround for a weird behavior.
            // https://github.com/jreleaser/jreleaser/issues/1292
            mkdir("$buildDir/jreleaser")
        }
        dependsOn("runtimeZip")
    }

    configure<JReleaserExtension> {
        gitRootSearch = true
        dryrun = false

        project {
            website = "https://smithy.io"
            authors = listOf("Smithy")
            vendor = "Smithy"
            license = "Apache-2.0"
            description = "Smithy CLI - A CLI for building, validating, querying, and iterating on Smithy models"
            copyright = "2019"
        }

        checksum {
            individual = true
            files = false
        }

        release {
            github {
                overwrite = true
                tagName = "{{projectVersion}}"
                skipTag = true
                releaseName = "Smithy CLI v{{{projectVersion}}}"
                changelog {
                    // For now, we won't have a changelog added to the release. In the future, we could create a changelog-snippet
                    // from the real changelog as part of a command hook prior to the release step
                    enabled = false
                }
                commitAuthor {
                    name = "smithy-automation"
                    email = "github-smithy-automation@amazon.com"
                }
            }
        }

        files {
            active = Active.ALWAYS
            artifact {
                // We'll include the VERSION file in the release artifacts so that the version can be easily
                // retrieving by hitting the GitHub `releases/latest` url
                setPath("../VERSION")
                extraProperties.put("skipSigning", true)
            }
        }

        platform {
            // These replacements are for the names of files that are released, *not* for names within this build config
            replacements =
                mapOf(
                    "osx" to "darwin",
                    "aarch_64" to "aarch64",
                    "windows_x86_64" to "windows_x64",
                )
        }

        distributions {
            create("smithy") {
                distributionType = Distribution.DistributionType.JLINK
                stereotype = Stereotype.CLI

                artifact {
                    path = layout.buildDirectory.file("image/smithy-cli-linux-x86_64.zip")
                    platform = ("linux-x86_64")
                }

                artifact {
                    path = layout.buildDirectory.file("image/smithy-cli-linux-aarch64.zip")
                    platform = ("linux-aarch_64")
                }

                artifact {
                    path = layout.buildDirectory.file("image/smithy-cli-darwin-x86_64.zip")
                    platform = ("osx-x86_64")
                }

                artifact {
                    path = layout.buildDirectory.file("image/smithy-cli-darwin-aarch64.zip")
                    platform = ("osx-aarch_64")
                }

                artifact {
                    path = layout.buildDirectory.file("image/smithy-cli-windows-x64.zip")
                    platform = ("windows-x86_64")
                }
            }
        }

        signing {
            active = Active.RELEASE
            armored = true
            verify = true
        }
    }
}

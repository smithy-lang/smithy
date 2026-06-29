/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import org.apache.tools.ant.taskdefs.condition.Os
import org.beryx.runtime.RuntimeTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.crypto.checksum.Checksum

plugins {
    application
    alias(libs.plugins.checksum)
    alias(libs.plugins.runtime)
    alias(libs.plugins.shadow)
    id("smithy.module-conventions")
    id("smithy.integ-test-conventions")
}

description = "This module implements the Smithy command line interface."

// EXPERIMENTAL(call): override the Java 8 release set by smithy.java-conventions because the
// bundled smithy-java runtime requires Java 21. Tests reference the call command (and thus the same
// smithy-java deps), so the test source set needs the same release.
tasks.compileJava {
    options.release.set(21)
}
tasks.compileTestJava {
    options.release.set(21)
}

extra["displayName"] = "Smithy :: CLI"
extra["moduleName"] = "software.amazon.smithy.cli"

configurePublishing {
    customComponent = components["shadow"]
}

// JDK 25 is required for the Leyden AOT cache (JEP 483/514: ahead-of-time class loading & linking),
// which the runtime image uses in place of the older AppCDS archive to speed up CLI startup.
// EXPERIMENTAL(call): smithy-java independently requires Java 21, so the bundled JRE must be at
// least 21 anyway; 25 satisfies both requirements.
val imageJreVersion = "25"
val correttoRoot = "https://corretto.aws/downloads/latest/amazon-corretto-$imageJreVersion"

// Building the runtime *image* requires JDK 25+: it bundles a JDK 25 JRE and generates a JDK 25 AOT
// cache, and jlink/the AOT steps need a matching toolchain. This only applies to the image tasks, not
// to compiling or testing the CLI (which, like the rest of the monorepo, build on 17+), so the check is
// enforced lazily per-task rather than at configuration time -- otherwise plain `build`/`test` would
// fail on an older JDK that's perfectly fine for them.
fun requireJava25ForImage() {
    val current = JavaVersion.current()
    check(current.isCompatibleWith(JavaVersion.VERSION_25)) {
        "Building the Smithy CLI runtime image requires Java 25 or later (for the AOT cache). " +
            "You are currently running Java ${current.majorVersion}."
    }
}

dependencies {
    constraints {
        implementation("org.codehaus.plexus:plexus-utils:3.6.1") {
            because(
                "CVE-2025-67030: directory traversal in Expand.extractFile (CVSS 8.8), fixed in 3.6.1 via https://github.com/codehaus-plexus/plexus-utils/pull/304",
            )
        }
    }

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

    // ===== EXPERIMENTAL: `smithy call` command =====
    // AWS + Smithy trait packages needed to load real AWS models with full fidelity (endpoint
    // rules, waiters, IAM, CloudFormation, protocols). These are sibling projects in this repo.
    //
    // Like smithy-model/build/diff/syntax above, these are declared as both `implementation` (for
    // compilation) and `shadow` (so they land in the runtime image), and are EXCLUDED from the
    // shaded jar below. They must stay as separate jars in the image: each Smithy jar ships a
    // `META-INF/smithy/manifest` listing its trait models, and shading would collide those manifests
    // so only one jar's models would be discoverable.
    val callTraits = listOf(
        ":smithy-aws-traits",
        ":smithy-protocol-traits",
        ":smithy-rules-engine",
        ":smithy-aws-endpoints",
        ":smithy-waiters",
        ":smithy-aws-iam-traits",
        ":smithy-aws-cloudformation-traits",
    )
    callTraits.forEach {
        implementation(project(it))
        shadow(project(it))
    }

    // smithy-java runtime that actually performs the call. The auth modules (aws-sigv4,
    // aws-credential-chain, aws-config) are NOT on Maven Central yet, so this resolves the whole
    // set from Maven local (~/.m2) at the locally-published version. Native auth only -- no AWS
    // SDK v2 bridge. Kept as separate jars in the image (not shaded) for the same manifest reason.
    val smithyJavaVersion = "1.4.0"
    val smithyJava = listOf(
        "dynamic-client",
        "client-http",
        "json-codec",
        "aws-client-restjson",
        "aws-client-awsjson",
        "aws-client-restxml",
        "client-rpcv2-cbor",
        "client-rpcv2-json",
        "aws-client-core",
        "aws-client-rulesengine", // AWS endpoint resolution at runtime
        "aws-client-s3", // S3 virtual-host bucket addressing (AutoClientPlugin)
        "aws-sigv4", // SigV4 signer + SigV4AuthScheme
        "aws-credential-chain", // native profile/env credentials (pulls aws-config, aws-auth-api, auth-api)
        "http-api", // HttpResponse, used by the --debug response dumper
        "io", // DataStream, used by the --debug response dumper
        "rulesengine", // Bytecode / RulesEngineBuilder, for precompiling endpoint rules at registration
        "client-rulesengine", // RulesEngineSettings.BYTECODE context key
        "jmespath", // --query: JMESPath filtering of output documents
        "cbor-codec", // compact CBOR encoding of --continue pagination tokens
    )
    smithyJava.forEach {
        implementation("software.amazon.smithy.java:$it:$smithyJavaVersion")
        shadow("software.amazon.smithy.java:$it:$smithyJavaVersion")
    }

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
        layout.buildDirectory
            .file("image/smithy-cli-$imageOs/bin/smithy.bat")
            .get()
            .asFile.path
    } else {
        layout.buildDirectory
            .file("image/smithy-cli-$imageOs/bin/smithy")
            .get()
            .asFile.path
    }
System.setProperty("SMITHY_BINARY", smithyBinary)

runtime {
    addOptions("--compress", "2", "--strip-debug", "--no-header-files", "--no-man-pages")
    // EXPERIMENTAL(call): java.net.http is required by smithy-java's JDK HttpClient transport.
    addModules("java.logging", "java.xml", "java.naming", "jdk.crypto.ec", "java.net.http")

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
                // Leyden AOT cache: classes are loaded AND linked from this cache, which is faster than the
                // old AppCDS archive (load-only). The cache is generated by the `optimize` task below after
                // the image is built.
                "-XX:AOTCache={{BIN_DIR}}/../lib/smithy.aot",
                // A missing or incompatible cache is non-fatal -- the JVM loads classes normally -- but by
                // default it prints [error][aot] lines to stderr on every run. Silence that log so a cache
                // that failed to ship degrades quietly instead of spamming the user.
                "-Xlog:aot=off",
                // Cap JIT at C1: the CLI is short-lived, so C2's profiling/recompilation never pays off.
                // Combined with the AOT cache above, this measurably cuts startup. Override via JAVA_OPTS if needed.
                "-XX:TieredStopAtLevel=1",
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
        javaToolchains
            .launcherFor {
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
        relocate("org.objectweb", "software.amazon.smithy.cli.shaded.objectweb")
        relocate("com.google", "software.amazon.smithy.cli.shaded.google")

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

            // EXPERIMENTAL(call): keep the trait packages and the smithy-java runtime out of the
            // shaded jar so they remain separate jars in the runtime image. Shading them together
            // would collide each jar's META-INF/smithy/manifest, hiding most trait models.
            exclude(project(":smithy-aws-traits"))
            exclude(project(":smithy-protocol-traits"))
            exclude(project(":smithy-rules-engine"))
            exclude(project(":smithy-aws-endpoints"))
            exclude(project(":smithy-waiters"))
            exclude(project(":smithy-aws-iam-traits"))
            exclude(project(":smithy-aws-cloudformation-traits"))
            exclude(dependency("software.amazon.smithy.java:.*:.*"))
        }
    }

    val runtime by getting(RuntimeTask::class) {
        dependsOn(shadowJar)

        // The image bundles a JDK 25 JRE and jlink must match it; fail fast with a clear message if the
        // build is running on an older JDK. Checked here (not at configuration time) so plain compile/test
        // builds aren't blocked on JDK 25.
        doFirst { requireJava25ForImage() }

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

    // Generate the AOT cache for the local (host-platform) image after it's built, by running the hidden
    // `warmup` command against it. The cache is platform-specific and can only be produced by running
    // that platform's java, so this only covers the build host; the other released platforms generate
    // their cache at install time (the installer runs `smithy warmup`). This task mirrors that path so a
    // locally built image is fast too.
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

    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    // Unfortunately, runtime plugin doesn't model the images as outputs, so we have to hardcode this
    val imageZips =
        runtime.imageDir.files(
            "smithy-cli-darwin-aarch64.zip",
            "smithy-cli-darwin-x86_64.zip",
            "smithy-cli-linux-aarch64.zip",
            "smithy-cli-linux-x86_64.zip",
            "smithy-cli-windows-x64.zip",
        )

    // Generate a sha256 checksum file for each zip
    val checksumImages by registering(Checksum::class) {
        dependsOn(runtimeZip)
        checksumAlgorithm = Checksum.Algorithm.SHA256
        appendFileNameToChecksum.set(true)
        outputDirectory = runtime.imageDir
        inputFiles.setFrom(imageZips)
    }

    // Generate an ascii-armored sig file for each zip
    val signImages by registering(Sign::class) {
        dependsOn(checksumImages)
        sign(*imageZips.files.toTypedArray())
    }

    // A wrapper task generates, checksums, and signs the image zipfiles
    // Not necessary, but a little clearer than just signImages
    val images by registering {
        dependsOn(runtimeZip, checksumImages, signImages)
    }
}

import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active

/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

plugins {
    java
    alias(libs.plugins.jreleaser)
}

// Load the Smithy version from VERSION.
val libraryVersion = project.file("VERSION").readText().trim()
println("Smithy version: '$libraryVersion'")

// Verify Java version is 17+
// Since most plugins are not toolchain-aware, we can't rely on gradle toolchains, which means we'll have to enforce
// that the global java version (i.e. whatever JAVA_HOME is set to and what Gradle uses) is set to 17+
val javaVersion = JavaVersion.current()
check(javaVersion.isCompatibleWith(JavaVersion.VERSION_17)) {
    "Building this project requires Java 17 or later. You are currently running Java ${javaVersion.majorVersion}."
}

allprojects {
    group = "software.amazon.smithy"
    version = libraryVersion
}

// Consolidated Javadoc creation
afterEvaluate {
    tasks {
        javadoc {
            title = "Smithy API ${version}"
            setDestinationDir(layout.buildDirectory.dir("docs/javadoc/latest").get().asFile)
            source(subprojects.map { project(it.path).sourceSets.main.get().allJava })
            classpath = files(subprojects.map { project(it.path).sourceSets.main.get().compileClasspath })
            (options as StandardJavadocDocletOptions).apply {
                addStringOption("Xdoclint:-html", "-quiet")
            }
        }
    }
}

configure<JReleaserExtension> {
    dryrun = false

    release {
        generic {
            skipTag = true
        }
    }

    // Used to announce a release to configured announcers.
    // https://jreleaser.org/guide/latest/reference/announce/index.html
    announce {
        active = Active.NEVER
    }

    // Signing configuration.
    // https://jreleaser.org/guide/latest/reference/signing.html
    signing {
        active = Active.ALWAYS
        armored = true
    }

    // Configuration for deploying to Maven Central.
    // https://jreleaser.org/guide/latest/examples/maven/maven-central.html#_gradle
    deploy {
        maven {
            mavenCentral {
                create("maven-central") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(stagingDir().get().asFile.path)
                    maxRetries = 100
                    retryDelay = 60
                }
            }
        }
    }
}


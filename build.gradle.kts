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
    id("org.jreleaser") version "1.12.0" apply false
}

// Load the Smithy version from VERSION.
val libraryVersion = project.file("VERSION").readText().trim()
println("Smithy version: '$libraryVersion'")

allprojects {
    group = "software.amazon.smithy"
    version = libraryVersion
}

//// Build a consolidated javadoc of all subprojects.
//afterEvaluate {
//    tasks {
//        val allJavadoc by registering(Javadoc::class) {
//            title = "Smithy API ${version}"
//            setDestinationDir(file("${project.buildDir}/docs/javadoc/latest"))
//            source(subprojects.map { project(it.path).sourceSets.main.get().allJava })
//            classpath = files(subprojects.map { project(it.path).sourceSets.main.get().compileClasspath })
//        }
//    }
//}
//
//// We're using JReleaser in the smithy-cli subproject, so we want to have a flag to control
//// which JReleaser configuration to use to prevent conflicts
//if (project.hasProperty("release.main")) {
//    extensions.configure<org.jreleaser.gradle.plugin.JReleaserExtension> {
//        dryrun.set(false)
//
//        // Used for creating and pushing the version tag, but this configuration ensures that
//        // an actual GitHub release isn't created (since the CLI release does that)
//        release {
//            github {
//                skipRelease.set(true)
//                tagName.set("{{projectVersion}}")
//            }
//        }
//
//        // Used to announce a release to configured announcers.
//        // https://jreleaser.org/guide/latest/reference/announce/index.html
//        announce {
//            active.set(Active.NEVER)
//        }
//
//        // Signing configuration.
//        // https://jreleaser.org/guide/latest/reference/signing.html
//        signing {
//            active.set(Active.ALWAYS)
//            armored.set(true)
//        }
//
//        // Configuration for deploying to Maven Central.
//        // https://jreleaser.org/guide/latest/examples/maven/maven-central.html#_gradle
//        deploy {
//            maven {
//                nexus2 {
//                    create("maven-central") {
//                        active.set(Active.ALWAYS)
//                        url.set("https://aws.oss.sonatype.org/service/local")
//                        snapshotUrl.set("https://aws.oss.sonatype.org/content/repositories/snapshots")
//                        closeRepository.set(true)
//                        releaseRepository.set(true)
//                        stagingRepository(stagingDir().get().toString())
//                    }
//                }
//            }
//        }
//    }
//}

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
    id("org.jreleaser") version "1.15.0" apply false
}

// Load the Smithy version from VERSION.
val libraryVersion = project.file("VERSION").readText().trim()
println("Smithy version: '$libraryVersion'")

allprojects {
    group = "software.amazon.smithy"
    version = libraryVersion
}

// We're using JReleaser in the smithy-cli subproject, so we want to have a flag to control
// which JReleaser configuration to use to prevent conflicts
if (project.hasProperty("release.main")) {
    apply(plugin = "org.jreleaser")

    // Workaround for https://github.com/jreleaser/jreleaser/issues/1492
    tasks.register("clean")

    configure<JReleaserExtension> {
        dryrun = false

        // Used for creating and pushing the version tag, but this configuration ensures that
        // an actual GitHub release isn't created (since the CLI release does that)
        release {
            github {
                skipRelease = true
                tagName = "{{projectVersion}}"
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
                nexus2 {
                    create("maven-central") {
                        active = Active.ALWAYS
                        url = "https://aws.oss.sonatype.org/service/local"
                        snapshotUrl = "https://aws.oss.sonatype.org/content/repositories/snapshots"
                        closeRepository = true
                        releaseRepository = true
                        stagingRepository(stagingDir().get().asFile.path)
                    }
                }
            }
        }
    }
}

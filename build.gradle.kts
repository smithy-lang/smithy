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

import com.github.spotbugs.snom.Effort
import org.jreleaser.model.Active
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    `maven-publish`
    signing
    checkstyle
    jacoco
    id("com.github.spotbugs") version "6.0.8"
    id("io.codearte.nexus-staging") version "0.30.0"
    id("me.champeau.jmh") version "0.7.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jreleaser") version "1.12.0" apply false
}

// Load the Smithy version from VERSION.
val libraryVersion = project.file("VERSION").readText().trim()

println("Smithy version: '$libraryVersion'")

allprojects {
    group = "software.amazon.smithy"
    version = libraryVersion
}

// JReleaser publishes artifacts from a local staging repository, rather than maven local.
// https://jreleaser.org/guide/latest/examples/maven/staging-artifacts.html#_gradle
val stagingDirectory = rootProject.layout.buildDirectory.dir("staging")

subprojects {
    apply(plugin = "java-library")
    java {
        if (project.name == "smithy-docgen-core" || project.name == "smithy-docgen-test") {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        } else {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }
    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
        testImplementation("org.hamcrest:hamcrest:2.1")
        testCompileOnly("org.apiguardian:apiguardian-api:1.1.2")
    }

    // Reusable license copySpec for building JARs
    val licenseSpec = copySpec {
        from("${project.rootDir}/LICENSE")
        from("${project.rootDir}/NOTICE")
    }

    // Set up tasks that build source and javadoc jars.
    tasks.register<Jar>("sourcesJar") {
        metaInf.with(licenseSpec)
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }

    // Build a javadoc JAR too.
    tasks.register<Jar>("javadocJar") {
        metaInf.with(licenseSpec)
        from(tasks.javadoc)
        archiveClassifier.set("javadoc")
    }

    // Include an Automatic-Module-Name in all JARs.
    afterEvaluate {
        tasks.jar {
            metaInf.with(licenseSpec)
            inputs.property("moduleName", project.extra.get("moduleName"))
            manifest {
                attributes("Automatic-Module-Name" to project.extra.get("moduleName"))
            }
        }
    }

    // Always run javadoc after build.
    tasks["build"].dependsOn(tasks["javadoc"])

    // ==== Tests ====
    // https://docs.gradle.org/current/samples/sample_java_multi_project_with_junit5_tests.html
    tasks.test {
        useJUnitPlatform()
    }

    // Log on passed, skipped, and failed test events if the `-Plog-tests` property is set.
    // https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/logging/TestLoggingContainer.html
    if (project.hasProperty("log-tests")) {
        tasks.test {
            testLogging {
                events("passed", "skipped", "failed")
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }

    // ==== Maven ====
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "com.github.johnrengelman.shadow")

    // This is a little hacky, but currently needed to build a shadowed CLI JAR and smithy-syntax JAR with the same
    // customizations as other JARs.
    if (project.name != "smithy-cli" && project.name != "smithy-syntax") {
        tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
            isEnabled = false
        }
    }

    publishing {
        repositories {
            // JReleaser's `publish` task publishes to all repositories, so only configure one.
            maven {
                name = "localStaging"
                url = uri(stagingDirectory)
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                if (tasks.findByName("shadowJar")?.enabled == true) {
                    project.shadow.component(this)
                } else {
                    from(components["java"])
                }

                // Ship the source and javadoc jars.
                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])

                // Include extra information in the POMs.
                afterEvaluate {
                    pom {
                        name.set(project.ext["displayName"].toString())
                        description.set(project.description)
                        url.set("https://github.com/smithy-lang/smithy")
                        licenses {
                            license {
                                name.set("Apache License 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("smithy")
                                name.set("Smithy")
                                organization.set("Amazon Web Services")
                                organizationUrl.set("https://aws.amazon.com")
                                roles.add("developer")
                            }
                        }
                        scm {
                            url.set("https://github.com/smithy-lang/smithy.git")
                        }
                    }
                }
            }
        }

        // Don't sign the artifacts if we didn't get a key and password to use.
        if (project.hasProperty("signingKey") && project.hasProperty("signingPassword")) {
            signing {
                useInMemoryPgpKeys(
                    project.property("signingKey").toString(),
                    project.property("signingPassword").toString()
                )
                sign(publishing.publications["mavenJava"])
            }
        }
    }

    tasks.register<Copy>("copyMavenMetadataForDevelopment") {
        from("build/tmp/publishMavenJavaPublicationToMavenLocal") {
            rename("module-maven-metadata.xml", "maven-metadata.xml")
        }
        val wdir = "${System.getProperty("user.home")}/.m2/repository/software/amazon/smithy/${project.name}"
        into(wdir)
    }

    tasks.publishToMavenLocal {
        finalizedBy("copyMavenMetadataForDevelopment")
    }

    // ==== CheckStyle ====
    // https://docs.gradle.org/current/userguide/checkstyle_plugin.html
    apply(plugin = "checkstyle")
    tasks.named("checkstyleTest") {
        enabled = false
    }

    // ==== Code coverage ====
    // https://docs.gradle.org/current/userguide/jacoco_plugin.html
    apply(plugin = "jacoco")

    // report is always generated after tests run
    tasks.test {
        finalizedBy(tasks.jacocoTestReport)
    }

    // tests are required to run before generating the report
    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(false)
            csv.required.set(false)
            html.outputLocation.set(file("$buildDir/reports/jacoco"))
        }
    }

    // ==== Spotbugs ====
    // https://plugins.gradle.org/plugin/com.github.spotbugs
    apply(plugin = "com.github.spotbugs")
    // We don't need to lint tests.
    tasks.named("spotbugsTest") {
        enabled = false
    }
    // Configure the bug filter for spotbugs.
    spotbugs {
        effort.set(Effort.MAX)
        excludeFilter.set(file("${project.rootDir}/config/spotbugs/filter.xml"))
    }
}

// The root project doesn't produce a JAR.
tasks.named("jar") {
    enabled = false
}

// ==== Javadoc ====
afterEvaluate {
    tasks.javadoc {
        title = "Smithy API ${version}"
        setDestinationDir(file("${project.buildDir}/docs/javadoc/latest"))
        // Build a consolidated javadoc of all subprojects.
        source(subprojects.map { project(it.path).sourceSets.main.get().allJava })
        classpath = files(subprojects.map { project(it.path).sourceSets.main.get().compileClasspath })
    }
}

// Disable HTML doclint to work around heading tag sequence validation
// inconsistencies between JDK15 and earlier Java versions.
allprojects {
    tasks.withType<Javadoc> {
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:-html", "-quiet")
            // Fixed in JDK 12: https://bugs.openjdk.java.net/browse/JDK-8215291
            // --no-module-directories does not exist in JDK 8 and is removed in 13
            if (JavaVersion.current().run { isJava9 || isJava10 || isJava11 }) {
                addBooleanOption("-no-module-directories", true)
            }
        }
    }
}

// We're using JReleaser in the smithy-cli subproject, so we want to have a flag to control
// which JReleaser configuration to use to prevent conflicts
if (project.hasProperty("release.main")) {
    apply(plugin = "org.jreleaser")

    extensions.configure<org.jreleaser.gradle.plugin.JReleaserExtension> {
        dryrun.set(false)

        // Used for creating and pushing the version tag, but this configuration ensures that
        // an actual GitHub release isn't created (since the CLI release does that)
        release {
            github {
                skipRelease.set(true)
                tagName.set("{{projectVersion}}")
            }
        }

        // Used to announce a release to configured announcers.
        // https://jreleaser.org/guide/latest/reference/announce/index.html
        announce {
            active.set(Active.NEVER)
        }

        // Signing configuration.
        // https://jreleaser.org/guide/latest/reference/signing.html
        signing {
            active.set(Active.ALWAYS)
            armored.set(true)
        }

        // Configuration for deploying to Maven Central.
        // https://jreleaser.org/guide/latest/examples/maven/maven-central.html#_gradle
        deploy {
            maven {
                nexus2 {
                    create("maven-central") {
                        active.set(Active.ALWAYS)
                        url.set("https://aws.oss.sonatype.org/service/local")
                        snapshotUrl.set("https://aws.oss.sonatype.org/content/repositories/snapshots")
                        closeRepository.set(true)
                        releaseRepository.set(true)
                        stagingRepository(stagingDirectory.get().toString())
                    }
                }
            }
        }
    }
}
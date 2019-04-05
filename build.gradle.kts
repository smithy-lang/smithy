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
    `java-library`
    `maven-publish`
    checkstyle
    jacoco
    id("com.github.spotbugs") version "1.6.10"
}

// Set a global group ID and version on each project. This version might
// need to be overridden is a project ever needs to be version bumped out
// of band with the rest of the projects.
allprojects {
    group = "software.amazon.smithy"
    version = "0.1.1"
}

subprojects {
    val subproject = this

    /*
     * Java
     * ====================================================
     *
     * By default, build each subproject as a java library.
     * We can add if-statements around this plugin to change
     * how specific subprojects are built (for example, if
     * we build Sphinx subprojects with Gradle).
     */
    apply(plugin = "java-library")

    if (plugins.hasPlugin("java")) {
        java {
            sourceCompatibility = JavaVersion.VERSION_11
        }

        tasks.withType(JavaCompile::class) {
            options.encoding = "UTF-8"
        }

        // Use Junit5's test runner.
        tasks.withType<Test> {
            useJUnitPlatform()
        }

        // Apply junit 5 and hamcrest test dependencies to all java projects.
        dependencies {
            testCompile("org.junit.jupiter:junit-jupiter-api:5.4.0")
            testRuntime("org.junit.jupiter:junit-jupiter-engine:5.4.0")
            testCompile("org.junit.jupiter:junit-jupiter-params:5.4.0")
            testCompile("org.hamcrest:hamcrest:2.1")
        }

        // Reusable license copySpec
        val licenseSpec = copySpec {
            from("${project.rootDir}/LICENSE")
            from("${project.rootDir}/NOTICE")
        }

        // Set up tasks that build source and javadoc jars.
        tasks.register<Jar>("sourcesJar") {
            metaInf.with(licenseSpec)
            from(sourceSets.main.get().allJava)
            archiveClassifier.set("sources")
        }

        tasks.register<Jar>("javadocJar") {
            metaInf.with(licenseSpec)
            from(tasks.javadoc)
            archiveClassifier.set("javadoc")
        }

        // Configure jars to include license related info
        tasks.jar {
            metaInf.with(licenseSpec)
            inputs.property("moduleName", subproject.extra["moduleName"])
            manifest {
                attributes["Automatic-Module-Name"] = subproject.extra["moduleName"]
            }
        }

        // Always run javadoc after build.
        tasks["build"].finalizedBy(tasks["javadoc"])
    }

    /*
     * Maven
     * ====================================================
     *
     * Publish to Maven central.
     */
    if (plugins.hasPlugin("java")) {
        apply(plugin = "maven-publish")

        repositories {
            mavenLocal()
            maven {
                url = uri("http://repo.maven.apache.org/maven2")
            }
        }

        publishing {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])

                    // Ship the source and javadoc jars.
                    artifact(tasks["sourcesJar"])
                    artifact(tasks["javadocJar"])
                }
            }
        }
    }

    /*
     * CheckStyle
     * ====================================================
     *
     * Apply CheckStyle to source files but not tests.
     */
    if (plugins.hasPlugin("java")) {
        apply(plugin = "checkstyle")

        tasks["checkstyleTest"].enabled = false
    }

    /*
     * Code coverage
     * ====================================================
     *
     * Create code coverage reports after running tests.
     */
    if (plugins.hasPlugin("java")) {
        apply(plugin = "jacoco")

        // Always run the jacoco test report after testing.
        tasks["test"].finalizedBy(tasks["jacocoTestReport"])

        // Configure jacoco to generate an HTML report.
        tasks.jacocoTestReport {
            reports {
                xml.isEnabled = false
                csv.isEnabled = false
                html.destination = file("$buildDir/reports/jacoco")
            }
        }
    }

    /*
     * Spotbugs
     * ====================================================
     *
     * Run spotbugs against source files and configure suppressions.
     */
    if (plugins.hasPlugin("java")) {
        apply(plugin = "com.github.spotbugs")

        // We don't need to lint tests.
        tasks["spotbugsTest"].enabled = false

        // Configure the bug filter for spotbugs.
        tasks.withType(com.github.spotbugs.SpotBugsTask::class) {
            effort = "max"
            excludeFilterConfig = project.resources.text.fromFile("${project.rootDir}/config/spotbugs/filter.xml")
        }
    }
}

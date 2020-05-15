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
    signing
    checkstyle
    jacoco
    id("com.github.spotbugs") version "1.6.10"
    id("io.codearte.nexus-staging") version "0.21.0"
}

// Set a global group ID and version on each project. This version might
// need to be overridden is a project ever needs to be version bumped out
// of band with the rest of the projects.
allprojects {
    group = "software.amazon.smithy"
    version = "0.9.10"
}

// The root project doesn't produce a JAR.
tasks["jar"].enabled = false

// Load the Sonatype user/password for use in publishing tasks.
val sonatypeUser: String? by project
val sonatypePassword: String? by project

/*
 * Sonatype Staging Finalization
 * ====================================================
 *
 * When publishing to Maven Central, we need to close the staging
 * repository and release the artifacts after they have been
 * validated. This configuration is for the root project because
 * it operates at the "group" level.
 */
if (sonatypeUser != null && sonatypePassword != null){
    apply(plugin = "io.codearte.nexus-staging")

    nexusStaging {
        packageGroup = "software.amazon"
        stagingProfileId = "e789115b6c941"

        username = sonatypeUser
        password = sonatypePassword
    }
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

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    repositories {
        mavenLocal()
        mavenCentral()
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

    tasks.jar {
        metaInf.with(licenseSpec)
        inputs.property("moduleName", subproject.extra["moduleName"])
        manifest {
            attributes["Automatic-Module-Name"] = subproject.extra["moduleName"]
        }
    }

    // Always run javadoc after build.
    tasks["build"].dependsOn(tasks["javadoc"])

    /*
     * Maven
     * ====================================================
     *
     * Publish to Maven central.
     */
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    publishing {
        repositories {
            mavenCentral {
                url = uri("https://aws.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = sonatypeUser
                    password = sonatypePassword
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])

                // Ship the source and javadoc jars.
                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])

                // Include extra information in the POMs.
                afterEvaluate {
                    pom {
                        name.set(subproject.extra["displayName"].toString())
                        description.set(subproject.description)
                        url.set("https://github.com/awslabs/smithy")
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
                            url.set("https://github.com/awslabs/smithy.git")
                        }
                    }
                }
            }
        }

        // Don't sign the artifacts if we didn't get a key and password to use.
        val signingKey: String? by project
        val signingPassword: String? by project
        if (signingKey != null && signingPassword != null) {
            signing {
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(publishing.publications["mavenJava"])
            }
        }
    }

    /*
     * CheckStyle
     * ====================================================
     *
     * Apply CheckStyle to source files but not tests.
     */
    apply(plugin = "checkstyle")

    tasks["checkstyleTest"].enabled = false

    /*
     * Code coverage
     * ====================================================
     *
     * Create code coverage reports after running tests.
     */
    apply(plugin = "jacoco")

    // Always run the jacoco test report after testing.
    tasks["build"].dependsOn(tasks["jacocoTestReport"])

    // Configure jacoco to generate an HTML report.
    tasks.jacocoTestReport {
        reports {
            xml.isEnabled = false
            csv.isEnabled = false
            html.destination = file("$buildDir/reports/jacoco")
        }
    }

    /*
     * Tests
     * ====================================================
     *
     * Configure the running of tests.
     */
    // Log on passed, skipped, and failed test events if the `-Plog-tests` property is set.
    if (project.hasProperty("log-tests")) {
        tasks.test {
            testLogging.events("passed", "skipped", "failed")
        }
    }

    /*
     * Spotbugs
     * ====================================================
     *
     * Run spotbugs against source files and configure suppressions.
     */
    apply(plugin = "com.github.spotbugs")

    // We don't need to lint tests.
    tasks["spotbugsTest"].enabled = false

    // Configure the bug filter for spotbugs.
    tasks.withType<com.github.spotbugs.SpotBugsTask> {
        effort = "max"
        excludeFilterConfig = project.resources.text.fromFile("${project.rootDir}/config/spotbugs/filter.xml")
    }
}

/*
 * Javadoc
 * ====================================================
 *
 * Configure javadoc to collect sources and construct
 * its classpath from the main sourceset of each
 * subproject.
 */
tasks.javadoc {
    title = "Smithy API $version"
    setDestinationDir(file("$buildDir/docs/javadoc/latest"))
    source(project.subprojects.map {
        project(it.name).sourceSets.main.get().allJava
    })
    classpath = files(project.subprojects.map {
        project(it.name).sourceSets.main.get().compileClasspath
    })
}

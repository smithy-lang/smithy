
plugins {
    `maven-publish`
    signing
}

publishing {
    repositories {
        // JReleaser's `publish` task publishes to all repositories, so only configure one.
        maven {
            name = "localStaging"
            url = uri(stagingDir())
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            if (components.names.contains("shadow")) {
                // Use the shadow component if its exists
                from(components["shadow"])
            } else {
                // Otherwise, use the standard java component
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

tasks {
    val copyMavenMetadataForDevelopment by registering(Copy::class) {
        from(layout.buildDirectory.dir("tmp/publishMavenJavaPublicationToMavenLocal")) {
            rename("module-maven-metadata.xml", "maven-metadata.xml")
        }
        val wdir = "${System.getProperty("user.home")}/.m2/repository/software/amazon/smithy/${project.name}"
        into(wdir)
    }

    publishToMavenLocal {
        finalizedBy(copyMavenMetadataForDevelopment)
    }
}

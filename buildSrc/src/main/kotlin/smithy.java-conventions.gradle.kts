import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import com.github.spotbugs.snom.Effort

plugins {
    `java-library`
    jacoco
    id("com.github.spotbugs")
    id("smithy.formatting-conventions")
}

// Workaround per: https://github.com/gradle/gradle/issues/15383
val Project.libs get() = the<org.gradle.accessors.dm.LibrariesForLibs>()

repositories {
    mavenLocal()
    mavenCentral()
}

/*
 * Base Java Jar Tasks
 * ===================
 */
// Reusable license copySpec for building JARs
val licenseSpec = copySpec {
    from("${project.rootDir}/LICENSE")
    from("${project.rootDir}/NOTICE")
}

java {
    toolchain {
        setSourceCompatibility(8)
        setTargetCompatibility(8)
    }
}

tasks {
    // Set up tasks that build source and javadoc jars.
    val sourcesJar by registering(Jar::class) {
        metaInf.with(licenseSpec)
        from(sourceSets.main.map { it.allSource })
        archiveClassifier.set("sources")
    }

    javadoc {
        // Disable HTML doclint to work around heading tag sequence validation
        // inconsistencies between JDK15 and earlier Java versions.
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:-html", "-quiet")
            // Fixed in JDK 12: https://bugs.openjdk.java.net/browse/JDK-8215291
            // --no-module-directories does not exist in JDK 8 and is removed in 13
            if (JavaVersion.current().run { isJava9 || isJava10 || isJava11 }) {
                addBooleanOption("-no-module-directories", true)
            }
        }
    }

    // Always run javadoc after build.
    build {
        dependsOn(javadoc)
    }

    // Build a javadoc JAR too.
    val javadocJar by registering(Jar::class) {
        metaInf.with(licenseSpec)
        from(javadoc)
        archiveClassifier.set("javadoc")
    }

    // Include an Automatic-Module-Name in all JARs.
    afterEvaluate {
        jar {
            metaInf.with(licenseSpec)
            inputs.property("moduleName", project.extra.get("moduleName"))
            manifest {
                attributes("Automatic-Module-Name" to project.extra.get("moduleName"))
            }
        }
    }
}

/*
 * Common test configuration
 * =========================
 */
dependencies {
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.hamcrest)
    testCompileOnly(libs.apiguardian.api)
}

// https://docs.gradle.org/current/samples/sample_java_multi_project_with_junit5_tests.html
tasks.withType<Test> {
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

// ==== Code coverage ====
// https://docs.gradle.org/current/userguide/jacoco_plugin.html

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
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
    }
}

// ==== Spotbugs ====
// https://plugins.gradle.org/plugin/com.github.spotbugs

// We don't need to lint tests.
tasks.named("spotbugsTest") {
    enabled = false
}

// Configure the bug filter for spotbugs.
spotbugs {
    effort.set(Effort.MAX)
    excludeFilter.set(file("${project.rootDir}/config/spotbugs/filter.xml"))
}


/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "AWS specific components for managing endpoints in Smithy"

extra["displayName"] = "Smithy :: AWS Endpoints Components"
extra["moduleName"] = "software.amazon.smithy.aws.endpoints"

// Custom configuration for S3 model - kept separate from test classpath to avoid
// polluting other tests with S3 model discovery
val s3Model: Configuration by configurations.creating

dependencies {
    api(project(":smithy-aws-traits"))
    api(project(":smithy-diff"))
    api(project(":smithy-rules-engine"))
    api(project(":smithy-model"))
    api(project(":smithy-utils"))

    s3Model("software.amazon.api.models:s3:1.0.11")
}

// Integration test source set for tests that require the S3 model
// These tests require JDK 17+ due to the S3 model dependency
sourceSets {
    create("it") {
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations["itImplementation"].extendsFrom(configurations["testImplementation"])
configurations["itRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])
configurations["itImplementation"].extendsFrom(s3Model)

// Configure IT source set to compile with current JDK (17+)
tasks.named<JavaCompile>("compileItJava") {
    // Use current Java version instead of hardcoding to allow flexibility in CI
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests that require external models like S3"
    group = "verification"
    testClassesDirs = sourceSets["it"].output.classesDirs
    classpath = sourceSets["it"].runtimeClasspath
    dependsOn(tasks.jar)
    shouldRunAfter(tasks.test)

    // Pass build directory to tests
    systemProperty(
        "buildDir",
        layout.buildDirectory
            .get()
            .asFile.absolutePath,
    )
}

tasks.test {
    finalizedBy(integrationTest)
}

tasks.named("check") {
    dependsOn(integrationTest)
}

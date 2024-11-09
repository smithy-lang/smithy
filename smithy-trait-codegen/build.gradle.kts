/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Plugin for Generating Trait Code from Smithy Models"

ext {
    set("displayName", "Smithy :: Trait Code Generation")
    set("moduleName", "software.amazon.smithy.traitcodegen")
}

dependencies {
    implementation(project(":smithy-codegen-core"))
}

// Set up Integration testing source sets
sourceSets {
    create("it") {
        compileClasspath += sourceSets.main.get().output +
                configurations["testRuntimeClasspath"] +
                configurations["testCompileClasspath"]

        runtimeClasspath += output +
                compileClasspath +
                sourceSets.test.get().runtimeClasspath +
                sourceSets.test.get().output

        java {
            srcDir("${layout.buildDirectory.get()}/integ/")
        }

        resources {
            srcDirs(layout.buildDirectory.get().dir("generated-resources"))
        }
    }
}

// Execute building of trait classes using an executable class
// These traits will then be passed in to the integration test (it)
// source set
tasks.register<JavaExec>("generateTraits") {
    classpath = sourceSets.test.get().runtimeClasspath + sourceSets.test.get().output
    mainClass.set("software.amazon.smithy.traitcodegen.PluginExecutor")
}

// Copy generated META-INF files to a new generated-resources directory to
// make it easy to include as resource srcDir
val generatedMetaInf = File("$buildDir/integ/META-INF")
val destResourceDir = File("$buildDir/generated-resources/META-INF")
tasks.register<Copy>("copyGeneratedSrcs") {
    from(generatedMetaInf)
    into(destResourceDir)
    dependsOn("generateTraits")
}

// Add the integ test task
tasks.register<Test>("integ") {
    useJUnitPlatform()
    testClassesDirs = sourceSets["it"].output.classesDirs
    classpath = sourceSets["it"].runtimeClasspath
}

// Do not run checkstyle on generated trait classes
tasks["checkstyleIt"].enabled = false

// Force correct ordering so generated sources are available
tasks["compileItJava"].dependsOn("generateTraits")
tasks["compileItJava"].dependsOn("copyGeneratedSrcs")
tasks["processItResources"].dependsOn("copyGeneratedSrcs")
tasks["integ"].mustRunAfter("generateTraits")
tasks["integ"].mustRunAfter("copyGeneratedSrcs")

// Always run integ tests after base tests
tasks["test"].finalizedBy("integ")

// dont run spotbugs on integ tests
tasks["spotbugsIt"].enabled = false
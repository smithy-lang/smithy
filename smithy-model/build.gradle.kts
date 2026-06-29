/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    id("smithy.integ-test-conventions")
    id("smithy.fuzz-test-conventions")
    id("smithy.profiling-conventions")
}

description = "This module provides the core implementation of loading, validating, " +
    "traversing, mutating, and serializing a Smithy model."

extra["displayName"] = "Smithy :: Model"
extra["moduleName"] = "software.amazon.smithy.model"

val awsModelsBomCoords =
    libs.aws.api.models.bom
        .get()
        .let { "${it.module.group}:${it.module.name}:${it.version}" }

dependencies {
    api(project(":smithy-utils"))
    jmh(project(":smithy-utils"))
    jmh(platform(awsModelsBomCoords))
    jmh("software.amazon.api.models:ec2")
    jmh("software.amazon.api.models:s3")
    jmh("software.amazon.api.models:dynamodb")
    jmh("software.amazon.api.models:sts")
}

// --- Integration tests: AWS model round-trip ---
// Only resolve and run when explicitly requested via -PawsModelsTests
if (project.hasProperty("awsModelsTests")) {
    val bomConfig =
        configurations
            .detachedConfiguration(
                dependencies.create("$awsModelsBomCoords@pom"),
            ).apply { isTransitive = false }

    dependencies {
        itImplementation(platform(awsModelsBomCoords))
    }

    afterEvaluate {
        val bomFile = bomConfig.singleFile
        val doc =
            javax.xml.parsers.DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(bomFile)
        val deps = doc.getElementsByTagName("dependency")
        for (i in 0 until deps.length) {
            val dep = deps.item(i) as? org.w3c.dom.Element ?: continue
            val g = dep.getElementsByTagName("groupId").item(0)?.textContent ?: continue
            val a = dep.getElementsByTagName("artifactId").item(0)?.textContent ?: continue
            if (g == "software.amazon.api.models") {
                dependencies.add("itImplementation", "$g:$a")
            }
        }
    }

    tasks.named<Test>("integ") {
        systemProperty("awsModelsTests", "true")
        maxHeapSize = "4g"
    }
}

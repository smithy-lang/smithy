/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    id("smithy.integ-test-conventions")
}

description = "This module contains support for generating UML diagrams from Smithy models."

extra["displayName"] = "Smithy :: UMLGen"
extra["moduleName"] = "software.amazon.smithy.umlgen"

dependencies {
    implementation(project(":smithy-codegen-core"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

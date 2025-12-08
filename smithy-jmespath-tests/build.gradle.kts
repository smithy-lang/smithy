/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "Compliance tests for JMESPath"

extra["displayName"] = "Smithy :: JMESPath Tests"
extra["moduleName"] = "software.amazon.smithy.jmespathtests"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(libs.junit.jupiter.api)
    api(libs.junit.jupiter.params)
    api(project(":smithy-jmespath"))
    implementation(project(":smithy-utils"))
}

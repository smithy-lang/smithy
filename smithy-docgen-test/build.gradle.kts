/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
description = "This modules contains test cases for the Smithy DocGen project"

extra["displayName"] = "Smithy :: DocGen :: Test"
extra["moduleName"] = "software.amazon.smithy.docgen.test"

tasks.named<Jar>("jar") {
    enabled = false
}

dependencies {
    api(project(":smithy-docgen-core"))
    api(project(":smithy-aws-traits"))
    implementation(project(":smithy-docgen-core"))
    implementation(project(":smithy-aws-traits"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
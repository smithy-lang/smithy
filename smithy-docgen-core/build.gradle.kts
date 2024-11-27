/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "This module contains support for generating API documentation " +
        "based on Smithy models."


extra["displayName"] = "Smithy :: DocGen :: Core"
extra["moduleName"] = "software.amazon.smithy.docgen.core"

dependencies {
    api(project(":smithy-model"))
    api(project(":smithy-build"))
    api(project(":smithy-utils"))
    api(project(":smithy-codegen-core"))
    api(project(":smithy-linters"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
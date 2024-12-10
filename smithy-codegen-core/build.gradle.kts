/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides a code generation framework for generating clients, " +
    "servers, documentation, and other artifacts for various languages from Smithy models."

extra["displayName"] = "Smithy :: Code Generation Framework"
extra["moduleName"] = "software.amazon.smithy.codegen.core"

dependencies {
    api(project(":smithy-utils"))
    api(project(":smithy-model"))
    api(project(":smithy-build"))
}

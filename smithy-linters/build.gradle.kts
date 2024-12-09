/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides support for customizable linters declared in the " +
    "metadata section of a Smithy model."

extra["displayName"] = "Smithy :: Linters"
extra["moduleName"] = "software.amazon.smithy.linters"

dependencies {
    api(project(":smithy-model"))
    api(project(":smithy-utils"))
}

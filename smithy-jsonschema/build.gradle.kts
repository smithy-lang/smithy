/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module contains support for converting a Smithy model to JSON Schema."

extra["displayName"] = "Smithy :: JSON Schema Conversion"
extra["moduleName"] = "software.amazon.smithy.jsonschema"

dependencies {
    api(project(":smithy-utils"))
    api(project(":smithy-model"))
}

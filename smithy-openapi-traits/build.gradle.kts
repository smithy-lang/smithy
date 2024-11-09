/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "This module provides Smithy traits that are used in converting a Smithy model to OpenAPI."

ext {
    set("displayName", "Smithy :: OpenAPI Traits")
    set("moduleName", "software.amazon.smithy.openapi.traits")
}

dependencies {
    api(project(":smithy-model"))
}

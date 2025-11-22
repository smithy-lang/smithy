/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides Smithy traits that depend on JMESPath."

extra["displayName"] = "Smithy :: Contracts"
extra["moduleName"] = "software.amazon.smithy.contracts"

dependencies {
    api(project(":smithy-model"))
    api(project(":smithy-jmespath"))
    api(project(":smithy-build"))
    api(project(":smithy-utils"))
}

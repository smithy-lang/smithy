/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "This module provides the implementation of protocol traits for Smithy."

extra["displayName"] = "Smithy :: Protocol Traits"
extra["moduleName"] = "software.amazon.smithy.protocol.traits"

dependencies {
    api(project(":smithy-utils"))
    api(project(":smithy-model"))
}

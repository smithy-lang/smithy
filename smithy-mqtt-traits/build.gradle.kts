/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides the implementation of MQTT binding traits for Smithy."

extra["displayName"] = "Smithy :: MQTT Traits"
extra["moduleName"] = "software.amazon.smithy.mqtt.traits"

dependencies {
    api(project(":smithy-utils"))
    api(project(":smithy-model"))
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module detects differences between two Smithy models, identifying " +
    "changes that are safe and changes that are backward incompatible."

extra["displayName"] = "Smithy :: Diff"
extra["moduleName"] = "software.amazon.smithy.diff"

dependencies {
    api(project(":smithy-utils"))
    api(project(":smithy-model"))
}

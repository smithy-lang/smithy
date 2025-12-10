/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    id("smithy.profiling-conventions")
}

description = "Applications of JMESPath to the core Smithy model."

extra["displayName"] = "Smithy :: Model :: JMESPath"
extra["moduleName"] = "software.amazon.smithy.model.jmespath"

dependencies {
    api(project(":smithy-model"))
    api(project(":smithy-jmespath"))
    testImplementation(project(":smithy-jmespath-tests"))
}

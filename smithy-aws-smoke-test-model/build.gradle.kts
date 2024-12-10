/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "Defines shapes used by AWS for modeling smoke tests"

ext {
    extra["displayName"] = "Smithy :: AWS :: Smoke Test :: Model"
    extra["moduleName"] = "software.amazon.smithy.aws.smoketest.model"
}

dependencies {
    api(project(":smithy-smoke-test-traits"))
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    id("smithy.integ-test-conventions")
}

description = "This module contains support for generating API documentation based on Smithy models."

extra["displayName"] = "Smithy :: DocGen"
extra["moduleName"] = "software.amazon.smithy.docgen"

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    implementation(project(":smithy-codegen-core"))
    implementation(project(":smithy-linters"))

    itImplementation(project(":smithy-aws-traits"))
}

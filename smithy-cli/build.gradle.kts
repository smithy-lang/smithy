/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

description = "This module implements the Smithy command line interface."
extra["displayName"] = "Smithy :: CLI"
extra["moduleName"] = "software.amazon.smithy.cli"

plugins {
    application
    id("org.beryx.runtime") version "1.1.6"
}

dependencies {
    implementation(project(":smithy-model"))
    implementation(project(":smithy-build"))
    implementation(project(":smithy-linters"))
    implementation(project(":smithy-diff"))
}

application {
    mainClassName = "software.amazon.smithy.cli.SmithyCli"
    applicationName = "smithy"
    applicationDefaultJvmArgs = listOf("-XX:TieredStopAtLevel=2", "-Xshare:auto", "-XX:SharedArchiveFile=app-cds.jsa")
}

runtime {
    addOptions("--compress", "0", "--strip-debug", "--no-header-files", "--no-man-pages")
    addModules("java.logging")
}

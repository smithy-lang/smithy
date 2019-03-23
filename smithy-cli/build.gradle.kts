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

plugins {
    application
    id("org.beryx.jlink") version "2.4.0"
}

dependencies {
    implementation(project(":smithy-model"))
    implementation(project(":smithy-build"))
    implementation(project(":smithy-linters"))
    implementation(project(":smithy-diff"))
    implementation(project(":smithy-codegen-core"))
    implementation(project(":smithy-codegen-freemarker"))
}

application {
    mainClassName = "software.amazon.smithy.cli/software.amazon.smithy.cli.SmithyCli"
}

jlink {
    addOptions("--compress", "0", "--strip-debug", "--no-header-files", "--no-man-pages")
    launcher {
        name = "smithy"
        unixScriptTemplate = file("scripts/launcher.sh")
        jvmArgs = listOf("-XX:TieredStopAtLevel=2", "-Xshare:auto", "-XX:SharedArchiveFile=app-cds.jsa")
    }
}

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

module software.amazon.smithy.cli {
    requires java.logging;
    requires software.amazon.smithy.model;
    requires software.amazon.smithy.build;
    requires software.amazon.smithy.diff;
    requires software.amazon.smithy.linters;
    requires software.amazon.smithy.codegen.core;
    requires software.amazon.smithy.codegen.freemarker;
}

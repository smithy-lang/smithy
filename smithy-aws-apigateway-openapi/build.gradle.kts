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

description = "This module provides support for converting the Amazon API Gateway " +
        "Smithy traits when converting a Smithy model to OpenAPI3."

extra["displayName"] = "Smithy :: Amazon API Gateway OpenAPI Support"
extra["moduleName"] = "software.amazon.smithy.aws.apigateway.openapi"

dependencies {
    api(project(":smithy-aws-apigateway-traits"))
    api(project(":smithy-openapi"))
}

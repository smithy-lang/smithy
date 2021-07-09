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

package software.amazon.smithy.aws.apigateway.openapi;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.utils.IoUtils;

public class CloudFormationSubstitutionTest {
    @Test
    public void performsSubstitutionsByDefault() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cloudformation-substitutions.json"))
                .assemble()
                .unwrap();

        ObjectNode expected = Node.parse(
                IoUtils.readUtf8Resource(getClass(), "substitution-performed.json"))
                .expectObjectNode();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ObjectNode actual = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convertToNode(model);

        Node.assertEquals(actual, expected);
    }

    @Test
    public void pluginCanBeDisabled() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cloudformation-substitutions.json"))
                .assemble()
                .unwrap();

        ObjectNode expected = Node.parse(
                IoUtils.readUtf8Resource(getClass(), "substitution-not-performed.json"))
                .expectObjectNode();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
        apiGatewayConfig.setDisableCloudFormationSubstitution(true);
        config.putExtensions(apiGatewayConfig);

        ObjectNode actual = OpenApiConverter.create()
                .classLoader(getClass().getClassLoader())
                .config(config)
                .convertToNode(model);

        Node.assertEquals(expected, actual);
    }
}

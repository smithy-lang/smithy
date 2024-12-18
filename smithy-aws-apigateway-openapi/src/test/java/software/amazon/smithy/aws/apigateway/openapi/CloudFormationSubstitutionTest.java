/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.IoUtils;

public class AddIntegrationsTest {
    @Test
    public void addsIntegrations() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("integrations.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("integrations.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void addsIntegrationsWithoutCredentials() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("integrations-without-credentials.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("integrations-without-credentials.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void throwsOnInvalidIntegrationTraitForHttpApi() {
        OpenApiException thrown = assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler(getClass().getClassLoader())
                    .discoverModels(getClass().getClassLoader())
                    .addImport(getClass().getResource("invalid-integration-for-http-api.json"))
                    .assemble()
                    .unwrap();
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("smithy.example#Service"));
            ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
            apiGatewayConfig.setApiGatewayType(ApiGatewayConfig.ApiType.HTTP);
            config.putExtensions(apiGatewayConfig);
            OpenApiConverter.create().config(config).convertToNode(model);
        });

        assertThat(thrown.getMessage(),
                containsString("When the 'apiGatewayType' OpenAPI conversion setting is"
                        + " 'HTTP', a 'payloadFormatVersion' must be set on the aws.apigateway#integration trait."));
    }
}

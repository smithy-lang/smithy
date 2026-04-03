/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.utils.IoUtils;

public class CorsOriginsKeyTest {

    @Test
    public void restApiSelectsOriginByKey() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-origins-key-rest.smithy"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
        apiGatewayConfig.setCorsOriginKey("prod");
        config.putExtensions(apiGatewayConfig);

        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("cors-origins-key-rest.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void restApiThrowsWhenCorsOriginKeyNotSet() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-origins-key-rest.smithy"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));

        assertThrows(OpenApiException.class, () -> OpenApiConverter.create().config(config).convertToNode(model));
    }

    @Test
    public void restApiThrowsWhenCorsOriginKeyNotFound() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-origins-key-rest.smithy"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
        apiGatewayConfig.setCorsOriginKey("staging");
        config.putExtensions(apiGatewayConfig);

        assertThrows(OpenApiException.class, () -> OpenApiConverter.create().config(config).convertToNode(model));
    }

    @Test
    public void httpApiUsesAllOriginsAndIgnoresCorsOriginKey() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-origins-http.smithy"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
        apiGatewayConfig.setApiGatewayType(ApiGatewayConfig.ApiType.HTTP);
        // corsOriginKey is set but should be ignored for HTTP APIs.
        apiGatewayConfig.setCorsOriginKey("prod");
        config.putExtensions(apiGatewayConfig);
        config.setService(ShapeId.from("example.smithy#MyService"));

        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("cors-origins-http.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }
}

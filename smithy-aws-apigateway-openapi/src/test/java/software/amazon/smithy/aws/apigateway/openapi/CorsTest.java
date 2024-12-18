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
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

public class CorsTest {
    @Test
    public void corsIntegrationTest() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-model.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("cors-model.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void skipsExplicitlyDefinedOptionsOperations() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-explicit-options.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("cors-explicit-options.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void setsConfiguredAdditionalAllowedHeaders() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-model.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
        apiGatewayConfig.setAdditionalAllowedCorsHeaders(ListUtils.of("foo", "bar", "content-length"));
        config.putExtensions(apiGatewayConfig);
        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("cors-with-additional-headers.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    /**
     * This test asserts two things: First, it ensures that any existing CORS headers
     * set on an explicitly added API Gateway integration are not overwritten
     * (i.e., the "Access-Control-Allow-Origin" is "domain.com" instead of https://foo.com).
     * Next, it asserts that any other headers in the gateway response show up in the
     * injected Access-Control-Expose-Headers header.
     */
    @Test
    public void findsExistingGatewayHeaders() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-explicit-options.json"))
                .assemble()
                .unwrap();
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("cors-with-custom-gateway-response-headers.openapi.json")));

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));

        // Create an OpenAPI model.
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .addOpenApiMapper(new OpenApiMapper() {
                    @Override
                    public byte getOrder() {
                        return -127;
                    }

                    @Override
                    public OpenApi after(Context context, OpenApi openapi) {
                        // Inject a gateway response into the model.
                        return openapi.toBuilder()
                                .putExtension("x-amazon-apigateway-gateway-responses",
                                        Node.objectNodeBuilder()
                                                .withMember("ACCESS_DENIED",
                                                        Node.objectNode()
                                                                .withMember("statusCode", 403)
                                                                .withMember("responseParameters",
                                                                        Node.objectNode()
                                                                                .withMember(
                                                                                        "gatewayresponse.header.Access-Control-Allow-Origin",
                                                                                        "'domain.com'")
                                                                                .withMember(
                                                                                        "gatewayresponse.header.Foo",
                                                                                        "'baz'")))
                                                .build())
                                .build();
                    }
                })
                .convertToNode(model);

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void withPreflightIntegrationSync() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-with-multi-request-templates.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        config.setSyncCorsPreflightIntegration(true);
        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("cors-with-preflight-sync.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void withoutPreflightIntegrationSync() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-with-multi-request-templates.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("cors-without-preflight-sync.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }
}

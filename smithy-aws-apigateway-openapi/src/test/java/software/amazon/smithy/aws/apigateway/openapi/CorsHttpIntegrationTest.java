/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.utils.IoUtils;

public class CorsHttpIntegrationTest {
    @Test
    public void generatesCorsForHttpApis() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-model.json"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
        apiGatewayConfig.setApiGatewayType(ApiGatewayConfig.ApiType.HTTP);
        config.putExtensions(apiGatewayConfig);
        config.setService(ShapeId.from("example.smithy#MyService"));

        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("http-api-cors.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void generatesCorsForHttpApisWithNoExplicitValues() {
        // This test replaces the trait found in http-api-cors.openapi.json so
        // that no explicit allowed and exposed headers are provided, causing
        // the conversion to "*" for the corresponding CORS headers rather
        // than needing to enumerate them all.
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-model.json"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        model = transformer.mapShapes(model, shape -> {
            return shape.getTrait(CorsTrait.class)
                    .map(cors -> {
                        cors = cors.toBuilder()
                                .additionalAllowedHeaders(Collections.emptySet())
                                .additionalExposedHeaders(Collections.emptySet())
                                .build();
                        return Shape.shapeToBuilder(shape).addTrait(cors).build();
                    })
                    .orElse(shape);
        });

        OpenApiConfig config = new OpenApiConfig();
        ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
        apiGatewayConfig.setApiGatewayType(ApiGatewayConfig.ApiType.HTTP);
        config.putExtensions(apiGatewayConfig);
        config.setService(ShapeId.from("example.smithy#MyService"));

        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("http-api-cors-wildcards.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }
}

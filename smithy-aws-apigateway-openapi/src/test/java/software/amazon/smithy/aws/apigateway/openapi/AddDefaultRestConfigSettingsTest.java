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

public class AddDefaultRestConfigSettingsTest {
    @Test
    public void addsDefaultConfigSettings() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("greedy-labels-for-rest.json"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
        apiGatewayConfig.setApiGatewayType(ApiGatewayConfig.ApiType.REST);
        config.putExtensions(apiGatewayConfig);
        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);

        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("greedy-labels-for-rest.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }
}

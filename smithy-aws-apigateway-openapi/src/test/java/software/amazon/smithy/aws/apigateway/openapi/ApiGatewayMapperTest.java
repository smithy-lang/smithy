/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class ApiGatewayMapperTest {

    @Test
    public void onlyCallsMappersWhenApiTypeMatches() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cors-model.json"))
                .assemble()
                .unwrap();

        runTest(model, ApiGatewayConfig.ApiType.REST, true);
        runTest(model, ApiGatewayConfig.ApiType.DISABLED, false);
        runTest(model, ApiGatewayConfig.ApiType.HTTP, false);
    }

    private void runTest(Model model, ApiGatewayConfig.ApiType type, boolean present) {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
        apiGatewayConfig.setApiGatewayType(type);
        config.putExtensions(apiGatewayConfig);

        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);

        if (present) {
            assertThat(result.getMember("x-amazon-apigateway-gateway-responses"), not(Optional.empty()));
        } else {
            assertThat(result.getMember("x-amazon-apigateway-gateway-responses"), equalTo(Optional.empty()));
        }
    }
}

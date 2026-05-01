/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class AddGatewayResponsesTest {
    @Test
    public void addsGatewayResponses() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("gateway-responses.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);

        assertTrue(result.getExtension("x-amazon-apigateway-gateway-responses").isPresent());
        ObjectNode responses = result.getExtension("x-amazon-apigateway-gateway-responses")
                .get()
                .expectObjectNode();

        // Verify DEFAULT_4XX
        ObjectNode default4xx = responses.expectObjectMember("DEFAULT_4XX");
        assertThat(default4xx.expectStringMember("statusCode").getValue(), equalTo("400"));
        assertThat(default4xx.expectObjectMember("responseParameters")
                .expectStringMember("gatewayresponse.header.Access-Control-Allow-Origin").getValue(),
                equalTo("'*'"));

        // Verify DEFAULT_5XX
        ObjectNode default5xx = responses.expectObjectMember("DEFAULT_5XX");
        assertThat(default5xx.expectStringMember("statusCode").getValue(), equalTo("500"));
    }

    @Test
    public void gatewayResponsesTakePrecedenceOverCors() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("gateway-responses-cors-precedence.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);

        ObjectNode responses = result.getExtension("x-amazon-apigateway-gateway-responses")
                .get()
                .expectObjectNode();
        ObjectNode default4xx = responses.expectObjectMember("DEFAULT_4XX");
        ObjectNode params = default4xx.expectObjectMember("responseParameters");

        // The customer set Access-Control-Allow-Origin to https://custom.example.com
        // in @gatewayResponses. The @cors trait sets it to https://cors-default.example.com.
        // Gateway responses should take precedence.
        assertThat(
                params.expectStringMember("gatewayresponse.header.Access-Control-Allow-Origin").getValue(),
                equalTo("'https://custom.example.com'"));
    }
}

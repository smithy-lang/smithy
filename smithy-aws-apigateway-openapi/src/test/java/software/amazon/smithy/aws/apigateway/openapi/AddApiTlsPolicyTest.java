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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class AddApiTlsPolicyTest {
    @Test
    public void addsApiTlsPolicy() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("api-tls-policy.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);

        assertTrue(result.getExtension("x-amazon-apigateway-security-policy").isPresent());
        assertThat(result.getExtension("x-amazon-apigateway-security-policy")
                .get()
                .expectStringNode()
                .getValue(),
                equalTo("TLS_1_2"));

        assertTrue(result.getExtension("x-amazon-apigateway-endpoint-access-mode").isPresent());
        assertThat(result.getExtension("x-amazon-apigateway-endpoint-access-mode")
                .get()
                .expectStringNode()
                .getValue(),
                equalTo("STRICT"));
    }
}

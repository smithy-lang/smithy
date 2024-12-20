/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class AddApiKeySourceTest {
    @Test
    public void addsApiKeySource() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("api-key-source.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);
        String source = result.getExtension("x-amazon-apigateway-api-key-source")
                .get()
                .expectStringNode()
                .getValue();

        assertThat(source, equalTo("HEADER"));
    }
}

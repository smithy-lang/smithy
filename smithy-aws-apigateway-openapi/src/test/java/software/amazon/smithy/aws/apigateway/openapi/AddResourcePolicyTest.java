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
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class AddResourcePolicyTest {
    @Test
    public void addsResourcePolicy() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("resource-policy.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);

        assertTrue(result.getExtension("x-amazon-apigateway-policy").isPresent());
        Node policy = result.getExtension("x-amazon-apigateway-policy").get();
        assertThat(policy.expectObjectNode().expectStringMember("Version").getValue(),
                equalTo("2012-10-17"));
    }
}

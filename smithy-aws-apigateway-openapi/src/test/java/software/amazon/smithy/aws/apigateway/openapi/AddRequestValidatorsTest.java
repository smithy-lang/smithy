/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class AddRequestValidatorsTest {
    @Test
    public void addsRequestValidators() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("request-validators.json"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);

        assertThat(result.getExtension("x-amazon-apigateway-request-validator").get(), equalTo(Node.from("full")));
        ObjectNode validators = result.getExtension("x-amazon-apigateway-request-validators").get().expectObjectNode();
        assertTrue(validators.containsMember("body-only"));
        assertTrue(validators.containsMember("full"));
        assertFalse(validators.containsMember("params-only"));

        Optional<Node> operation1Val = result.getPaths()
                .get("/1")
                .getPut()
                .get()
                .getExtension("x-amazon-apigateway-request-validator");
        assertFalse(operation1Val.isPresent());

        Optional<Node> operation2Val = result.getPaths()
                .get("/2")
                .getPut()
                .get()
                .getExtension("x-amazon-apigateway-request-validator");
        assertThat(operation2Val.get(), equalTo(Node.from("body-only")));
    }
}

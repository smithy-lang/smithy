/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidatedResult;

public class ApiGatewayConfigTraitTest {
    @Test
    public void roundTripsFromNode() {
        ObjectNode node = Node.objectNodeBuilder()
                .withMember("version", "1")
                .build();
        ApiGatewayConfigTrait trait = ApiGatewayConfigTrait.builder()
                .version("1")
                .build();

        assertThat(trait.toNode(), equalTo(node));
        assertThat(new ApiGatewayConfigTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#Bar"), node),
                equalTo(trait));
    }

    @Test
    public void acceptsSupportedVersion() {
        ValidatedResult<Model> result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\n"
                                + "namespace smithy.example\n"
                                + "use aws.apigateway#apiGatewayConfig\n"
                                + "\n"
                                + "@apiGatewayConfig(version: \"1\")\n"
                                + "service Example {\n"
                                + "    version: \"2026-10-02\"\n"
                                + "}\n")
                .assemble();

        assertFalse(result.isBroken(), () -> result.getValidationEvents().toString());
        ServiceShape service = result.unwrap()
                .expectShape(ShapeId.from("smithy.example#Example"), ServiceShape.class);
        assertThat(service.expectTrait(ApiGatewayConfigTrait.class).getVersion(), equalTo("1"));
    }
}

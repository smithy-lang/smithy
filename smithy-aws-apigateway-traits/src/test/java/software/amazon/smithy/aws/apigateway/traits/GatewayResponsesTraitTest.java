/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class GatewayResponsesTraitTest {
    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        ShapeId id = ShapeId.from("smithy.example#Foo");
        ObjectNode node = Node.objectNodeBuilder()
                .withMember("DEFAULT_4XX", Node.objectNodeBuilder()
                        .withMember("statusCode", "400")
                        .withMember("responseTemplates", Node.objectNodeBuilder()
                                .withMember("application/json", "{\"message\": \"bad request\"}")
                                .build())
                        .build())
                .build();
        Trait trait = factory.createTrait(GatewayResponsesTrait.ID, id, node).get();

        assertThat(trait, instanceOf(GatewayResponsesTrait.class));
        assertThat(((GatewayResponsesTrait) trait).getValue(), equalTo(node));
        assertThat(factory.createTrait(GatewayResponsesTrait.ID, id, trait.toNode()).get(),
                equalTo(trait));
    }

    @Test
    public void roundTripsFromNode() {
        ObjectNode node = Node.objectNodeBuilder()
                .withMember("INVALID_API_KEY", Node.objectNodeBuilder()
                        .withMember("statusCode", "429")
                        .withMember("responseParameters", Node.objectNodeBuilder()
                                .withMember("gatewayresponse.header.Retry-After", "'60'")
                                .build())
                        .build())
                .build();
        GatewayResponsesTrait trait = new GatewayResponsesTrait(node);

        assertThat(trait.toNode(), equalTo(node));
        assertThat(new GatewayResponsesTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#Bar"), trait.toNode()),
                equalTo(trait));
    }
}

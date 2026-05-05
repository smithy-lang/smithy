/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
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
                .withMember("DEFAULT_4XX",
                        Node.objectNodeBuilder()
                                .withMember("statusCode", "400")
                                .withMember("responseTemplates",
                                        Node.objectNodeBuilder()
                                                .withMember("application/json", "{\"message\": \"bad request\"}")
                                                .build())
                                .build())
                .build();
        Trait trait = factory.createTrait(GatewayResponsesTrait.ID, id, node).get();

        assertThat(trait, instanceOf(GatewayResponsesTrait.class));
        GatewayResponsesTrait gatewayTrait = (GatewayResponsesTrait) trait;
        assertThat(gatewayTrait.getResponses().size(), is(1));
        assertThat(gatewayTrait.getResponse("DEFAULT_4XX").isPresent(), is(true));
        assertThat(gatewayTrait.getResponse("DEFAULT_4XX").get().getStatusCode(),
                equalTo(Optional.of("400")));
        assertThat(factory.createTrait(GatewayResponsesTrait.ID, id, trait.toNode()).get(),
                equalTo(trait));
    }

    @Test
    public void roundTripsFromNode() {
        ObjectNode node = Node.objectNodeBuilder()
                .withMember("INVALID_API_KEY",
                        Node.objectNodeBuilder()
                                .withMember("statusCode", "429")
                                .withMember("responseParameters",
                                        Node.objectNodeBuilder()
                                                .withMember("gatewayresponse.header.Retry-After", "'60'")
                                                .build())
                                .build())
                .build();
        GatewayResponsesTrait trait = GatewayResponsesTrait.builder()
                .putResponse("INVALID_API_KEY",
                        GatewayResponse.builder()
                                .statusCode("429")
                                .putResponseParameter("gatewayresponse.header.Retry-After", "'60'")
                                .build())
                .build();

        assertThat(trait.toNode(), equalTo(node));
        assertThat(new GatewayResponsesTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#Bar"), trait.toNode()),
                equalTo(trait));
    }

    @Test
    public void buildsWithBuilder() {
        GatewayResponsesTrait trait = GatewayResponsesTrait.builder()
                .putResponse("DEFAULT_4XX",
                        GatewayResponse.builder()
                                .statusCode("400")
                                .putResponseParameter("gatewayresponse.header.Access-Control-Allow-Origin",
                                        "'https://example.com'")
                                .putResponseTemplate("application/json", "{\"message\": \"bad request\"}")
                                .build())
                .putResponse("DEFAULT_5XX",
                        GatewayResponse.builder()
                                .statusCode("500")
                                .build())
                .build();

        assertThat(trait.getResponses().size(), is(2));

        GatewayResponse response4xx = trait.getResponse("DEFAULT_4XX").get();
        assertThat(response4xx.getStatusCode(), equalTo(Optional.of("400")));
        assertThat(response4xx.getResponseParameters().get("gatewayresponse.header.Access-Control-Allow-Origin"),
                equalTo("'https://example.com'"));
        assertThat(response4xx.getResponseTemplates().get("application/json"),
                equalTo("{\"message\": \"bad request\"}"));

        GatewayResponse response5xx = trait.getResponse("DEFAULT_5XX").get();
        assertThat(response5xx.getStatusCode(), equalTo(Optional.of("500")));
    }

    @Test
    public void convertsToBuilder() {
        GatewayResponsesTrait trait = GatewayResponsesTrait.builder()
                .putResponse("DEFAULT_4XX",
                        GatewayResponse.builder()
                                .statusCode("400")
                                .build())
                .build();

        GatewayResponsesTrait rebuilt = trait.toBuilder().build();
        assertThat(rebuilt, equalTo(trait));
    }
}

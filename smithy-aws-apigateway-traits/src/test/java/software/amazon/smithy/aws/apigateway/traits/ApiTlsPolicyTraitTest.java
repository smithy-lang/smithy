/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class ApiTlsPolicyTraitTest {
    @Test
    public void loadsTraitWithAllFields() {
        ApiTlsPolicyTrait trait = ApiTlsPolicyTrait.builder()
                .securityPolicy("SecurityPolicy_TLS13_1_3_2025_09")
                .endpointAccessMode("STRICT")
                .build();

        assertThat(trait.getSecurityPolicy(), equalTo("SecurityPolicy_TLS13_1_3_2025_09"));
        assertThat(trait.getEndpointAccessMode(), equalTo(Optional.of("STRICT")));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void loadsTraitWithRequiredFieldOnly() {
        ApiTlsPolicyTrait trait = ApiTlsPolicyTrait.builder()
                .securityPolicy("TLS_1_0")
                .build();

        assertThat(trait.getSecurityPolicy(), equalTo("TLS_1_0"));
        assertThat(trait.getEndpointAccessMode(), equalTo(Optional.empty()));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void roundTripsFromNode() {
        ApiTlsPolicyTrait trait = ApiTlsPolicyTrait.builder()
                .securityPolicy("SecurityPolicy_TLS13_1_2_2021_06")
                .endpointAccessMode("BASIC")
                .build();

        assertThat(new ApiTlsPolicyTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#Bar"), trait.toNode()),
                equalTo(trait));
    }

    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        ShapeId id = ShapeId.from("smithy.example#Foo");
        ObjectNode node = Node.objectNodeBuilder()
                .withMember("securityPolicy", "TLS_1_2")
                .withMember("endpointAccessMode", "BASIC")
                .build();
        Trait trait = factory.createTrait(ApiTlsPolicyTrait.ID, id, node).get();

        assertThat(trait, instanceOf(ApiTlsPolicyTrait.class));
        assertThat(factory.createTrait(ApiTlsPolicyTrait.ID, id, trait.toNode()).get(), equalTo(trait));
    }
}

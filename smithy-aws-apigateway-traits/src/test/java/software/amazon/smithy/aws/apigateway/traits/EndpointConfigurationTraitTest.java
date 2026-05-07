/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class EndpointConfigurationTraitTest {
    @Test
    public void loadsTraitWithAllFields() {
        EndpointConfigurationTrait trait = EndpointConfigurationTrait.builder()
                .types(Arrays.asList("PRIVATE"))
                .vpcEndpointIds(Arrays.asList("vpce-0212a4ababd5b8c3e", "vpce-01d622316a7df47f9"))
                .disableExecuteApiEndpoint(true)
                .build();

        assertThat(trait.getTypes(), equalTo(Arrays.asList("PRIVATE")));
        assertThat(trait.getVpcEndpointIds(),
                equalTo(Optional.of(Arrays.asList("vpce-0212a4ababd5b8c3e", "vpce-01d622316a7df47f9"))));
        assertThat(trait.getDisableExecuteApiEndpoint(), equalTo(Optional.of(true)));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void loadsTraitWithRequiredFieldOnly() {
        EndpointConfigurationTrait trait = EndpointConfigurationTrait.builder()
                .types(Arrays.asList("REGIONAL"))
                .build();

        assertThat(trait.getTypes(), equalTo(Arrays.asList("REGIONAL")));
        assertThat(trait.getVpcEndpointIds(), equalTo(Optional.empty()));
        assertThat(trait.getDisableExecuteApiEndpoint(), equalTo(Optional.empty()));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void roundTripsFromNode() {
        EndpointConfigurationTrait trait = EndpointConfigurationTrait.builder()
                .types(Arrays.asList("EDGE"))
                .disableExecuteApiEndpoint(false)
                .build();

        assertThat(new EndpointConfigurationTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#Bar"), trait.toNode()),
                equalTo(trait));
    }

    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        ShapeId id = ShapeId.from("smithy.example#Foo");
        ObjectNode node = Node.objectNodeBuilder()
                .withMember("types", Node.fromStrings("REGIONAL"))
                .withMember("vpcEndpointIds", Node.fromStrings("vpce-abc123"))
                .withMember("disableExecuteApiEndpoint", true)
                .build();
        Trait trait = factory.createTrait(EndpointConfigurationTrait.ID, id, node).get();

        assertThat(trait, instanceOf(EndpointConfigurationTrait.class));
        assertThat(factory.createTrait(EndpointConfigurationTrait.ID, id, trait.toNode()).get(),
                equalTo(trait));
    }
}

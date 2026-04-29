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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class MinimumCompressionSizeTraitTest {
    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        ShapeId id = ShapeId.from("smithy.example#Foo");
        Trait trait = factory.createTrait(MinimumCompressionSizeTrait.ID, id, Node.from(10240)).get();

        assertThat(trait, instanceOf(MinimumCompressionSizeTrait.class));
        assertThat(((MinimumCompressionSizeTrait) trait).getValue(), equalTo(10240));
        assertThat(factory.createTrait(MinimumCompressionSizeTrait.ID, id, trait.toNode()).get(), equalTo(trait));
    }

    @Test
    public void roundTripsFromNode() {
        MinimumCompressionSizeTrait trait = new MinimumCompressionSizeTrait(0);

        assertThat(trait.getValue(), equalTo(0));
        assertThat(trait.toNode(), equalTo(Node.from(0)));
        assertThat(new MinimumCompressionSizeTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#Bar"), trait.toNode()),
                equalTo(trait));
    }

    @Test
    public void roundTripsMaxValue() {
        MinimumCompressionSizeTrait trait = new MinimumCompressionSizeTrait(10485760);

        assertThat(trait.getValue(), equalTo(10485760));
        assertThat(new MinimumCompressionSizeTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#Bar"), trait.toNode()),
                equalTo(trait));
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class CfnMutabilityTraitTest {

    @Test
    public void loadsTraitWithString() {
        Node node = Node.from("full");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("aws.cloudformation#cfnMutability"),
                ShapeId.from("ns.qux#Foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(CfnMutabilityTrait.class));
        CfnMutabilityTrait cfnMutabilityTrait = (CfnMutabilityTrait) trait.get();
        assertThat(cfnMutabilityTrait.getValue(), equalTo("full"));
        assertTrue(cfnMutabilityTrait.isFullyMutable());
        assertThat(cfnMutabilityTrait.toNode(), equalTo(node));
    }

}

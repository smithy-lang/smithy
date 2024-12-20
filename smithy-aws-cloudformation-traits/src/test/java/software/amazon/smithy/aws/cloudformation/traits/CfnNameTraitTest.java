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

public final class CfnNameTraitTest {

    @Test
    public void loadsTraitWithString() {
        Node node = Node.from("Text");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("aws.cloudformation#cfnName"),
                ShapeId.from("ns.qux#Foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(CfnNameTrait.class));
        CfnNameTrait cfnNameTrait = (CfnNameTrait) trait.get();
        assertThat(cfnNameTrait.getValue(), equalTo("Text"));
        assertThat(cfnNameTrait.toNode(), equalTo(node));
    }
}

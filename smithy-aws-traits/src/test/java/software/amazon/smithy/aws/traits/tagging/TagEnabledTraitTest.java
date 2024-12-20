/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class TagEnabledTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Map<StringNode, Node> properties = new HashMap<>();
        properties.put(StringNode.from("disableDefaultOperations"), Node.from(true));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("aws.api#tagEnabled"),
                ShapeId.from("ns.qux#foo"),
                Node.objectNode(properties));

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(TagEnabledTrait.class));
        TagEnabledTrait typedTrait = (TagEnabledTrait) trait.get();
        assertTrue(typedTrait.getDisableDefaultOperations());
    }

    @Test
    public void loadsTraitDefaultCheck() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("aws.api#tagEnabled"),
                ShapeId.from("ns.qux#foo"),
                Node.objectNode());

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(TagEnabledTrait.class));
        TagEnabledTrait typedTrait = (TagEnabledTrait) trait.get();
        assertFalse(typedTrait.getDisableDefaultOperations());
    }
}

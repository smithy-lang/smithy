/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

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

public class TopicLabelTraitTest {
    @Test
    public void loadsTrait() {
        ShapeId id = ShapeId.from("foo.bar#Baz$bam");
        Node node = Node.objectNode();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(TopicLabelTrait.ID, id, node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(TopicLabelTrait.class));
        TopicLabelTrait labelTrait = (TopicLabelTrait) trait.get();
        assertThat(new TopicLabelTrait.Provider().createTrait(id, labelTrait.toNode()), equalTo(labelTrait));
    }
}

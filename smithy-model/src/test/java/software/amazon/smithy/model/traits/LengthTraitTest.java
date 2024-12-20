/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class LengthTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Map<StringNode, Node> values = new HashMap<>();
        values.put(Node.from("min"), Node.from(1L));
        values.put(Node.from("max"), Node.from(10L));
        Node node = Node.objectNode(values);
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#length"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(LengthTrait.class));
        LengthTrait lengthTrait = (LengthTrait) trait.get();
        assertTrue(lengthTrait.getMin().isPresent());
        assertTrue(lengthTrait.getMax().isPresent());
        assertThat(lengthTrait.getMin().get(), equalTo(1L));
        assertThat(lengthTrait.getMax().get(), equalTo(10L));
        assertThat(lengthTrait.toNode(), equalTo(node));
        assertThat(lengthTrait.toBuilder().build(), equalTo(lengthTrait));
    }

    @Test
    public void requiresOneOfMinOrMax() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            Map<StringNode, Node> values = new HashMap<>();

            provider.createTrait(ShapeId.from("smithy.api#length"),
                    ShapeId.from("ns.qux#foo"),
                    Node.objectNode(values));
        });
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class RangeTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Map<StringNode, Node> values = new HashMap<>();
        values.put(Node.from("min"), Node.from(1L));
        values.put(Node.from("max"), Node.from(10L));
        Node node = Node.objectNode(values);
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#range"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(RangeTrait.class));
        RangeTrait rangeTrait = (RangeTrait) trait.get();
        assertTrue(rangeTrait.getMin().isPresent());
        assertTrue(rangeTrait.getMax().isPresent());
        assertThat(rangeTrait.getMin().get(), equalTo(BigDecimal.ONE));
        assertThat(rangeTrait.getMax().get(), equalTo(BigDecimal.TEN));
        assertThat(rangeTrait.toNode(), equalTo(node));
        assertThat(rangeTrait.toBuilder().build(), equalTo(rangeTrait));
    }

    @Test
    public void loadsTraitFromNotation() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Map<StringNode, Node> values = new HashMap<>();
        values.put(Node.from("min"), Node.from(1e0));
        values.put(Node.from("max"), Node.from(10e0));
        Node node = Node.objectNode(values);
        Optional<Trait> trait =
                provider.createTrait(ShapeId.from("smithy.api#range"), ShapeId.from("ns.qux#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(RangeTrait.class));
        RangeTrait rangeTrait = (RangeTrait) trait.get();
        assertTrue(rangeTrait.getMin().isPresent());
        assertTrue(rangeTrait.getMax().isPresent());
        assertThat(rangeTrait.getMin().get(), equalTo(new BigDecimal("1.0")));
        assertThat(rangeTrait.getMax().get(), equalTo(new BigDecimal("10.0")));
        assertThat(rangeTrait.toNode(), equalTo(node));
        assertThat(rangeTrait.toBuilder().build(), equalTo(rangeTrait));
    }

    @Test
    public void requiresOneOfMinOrMax() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            Map<StringNode, Node> values = new HashMap<>();
            provider.createTrait(ShapeId.from("smithy.api#range"), ShapeId.from("ns.qux#foo"), Node.objectNode(values));
        });
    }
}

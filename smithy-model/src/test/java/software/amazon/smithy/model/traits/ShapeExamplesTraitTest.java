/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class ShapeExamplesTraitTest {
    @ParameterizedTest
    @MethodSource("nodeValues")
    public void loadsTrait(ObjectNode node) {
        TraitFactory provider = TraitFactory.createServiceFactory();

        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#shapeExamples"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ShapeExamplesTrait.class));
        ShapeExamplesTrait shapeExamplesTrait = (ShapeExamplesTrait) trait.get();

        assertThat(shapeExamplesTrait.toNode(), equalTo(node));
        assertThat(shapeExamplesTrait.toBuilder().build(), equalTo(shapeExamplesTrait));
    }

    public static List<ObjectNode> nodeValues() {
        return Arrays.asList(
                Node.objectNode()
                        .withMember("allowed", Node.fromStrings("a"))
                        .withMember("disallowed", Node.fromStrings("b")),
                Node.objectNode()
                        .withMember("disallowed", Node.fromStrings("b")),
                Node.objectNode()
                        .withMember("allowed", Node.fromStrings("a")));
    }

    @Test
    public void requiresOneOfAllowedOrDisallowed() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            ObjectNode node = Node.objectNode();
            provider.createTrait(ShapeId.from("smithy.api#shapeExamples"), ShapeId.from("ns.qux#foo"), node);
        });
    }

    @Test
    public void requiresNonEmptyAllowedListIfDefined() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            ObjectNode node = Node.objectNode()
                    .withMember("allowed", Node.arrayNode())
                    .withMember("disallowed", Node.fromStrings("b"));
            provider.createTrait(ShapeId.from("smithy.api#shapeExamples"), ShapeId.from("ns.qux#foo"), node);
        });
    }

    @Test
    public void requiresNonEmptyDisallowedListIfDefined() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            ObjectNode node = Node.objectNode()
                    .withMember("allowed", Node.fromStrings("a"))
                    .withMember("disallowed", Node.arrayNode());
            provider.createTrait(ShapeId.from("smithy.api#shapeExamples"), ShapeId.from("ns.qux#foo"), node);
        });
    }
}

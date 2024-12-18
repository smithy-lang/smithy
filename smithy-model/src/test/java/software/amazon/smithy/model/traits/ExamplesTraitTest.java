/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ExamplesTrait.ErrorExample;
import software.amazon.smithy.model.traits.ExamplesTrait.Example;

public class ExamplesTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ArrayNode node = Node.arrayNode(
                Node.objectNode()
                        .withMember("title", Node.from("foo")),
                Node.objectNode()
                        .withMember("title", Node.from("qux"))
                        .withMember("documentation", Node.from("docs"))
                        .withMember("input", Node.objectNode().withMember("a", Node.from("b")))
                        .withMember("output", Node.objectNode().withMember("c", Node.from("d")))
                        .withMember("error",
                                Node.objectNode()
                                        .withMember(Node.from("shapeId"), Node.from("smithy.example#FooError"))
                                        .withMember(Node.from("content"),
                                                Node.objectNode().withMember("e", Node.from("f"))))
                        .withMember("allowConstraintErrors", Node.from(true)));

        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#examples"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ExamplesTrait.class));
        ExamplesTrait examples = (ExamplesTrait) trait.get();

        assertThat(examples.toNode(), equalTo(node));
        assertThat(examples.toBuilder().build(), equalTo(examples));
    }

    @Test
    public void omitsAllowConstraintErrorsFromSerializedNodeWhenNotTrue() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ArrayNode node = Node.arrayNode(
                Node.objectNode()
                        .withMember("title", Node.from("foo")),
                Node.objectNode()
                        .withMember("title", Node.from("qux"))
                        .withMember("documentation", Node.from("docs"))
                        .withMember("input", Node.objectNode().withMember("a", Node.from("b")))
                        .withMember("output", Node.objectNode().withMember("c", Node.from("d")))
                        .withMember("allowConstraintErrors", Node.from(false)),
                Node.objectNode()
                        .withMember("title", Node.from("qux"))
                        .withMember("documentation", Node.from("docs"))
                        .withMember("input", Node.objectNode().withMember("a", Node.from("b")))
                        .withMember("output", Node.objectNode().withMember("c", Node.from("d")))
                        .withMember("allowConstraintErrors", Node.from(true)));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#examples"),
                ShapeId.from("ns.qux#foo"),
                node);
        ArrayNode serialized = ((ExamplesTrait) trait.get()).createNode().expectArrayNode();

        assertFalse(serialized.get(0).get().asObjectNode().get().getMember("allowConstraintErrors").isPresent());
        assertFalse(serialized.get(1).get().asObjectNode().get().getMember("allowConstraintErrors").isPresent());
        assertTrue(serialized.get(2).get().asObjectNode().get().getMember("allowConstraintErrors").isPresent());
    }

    @Test
    public void exampleEqualsWorks() {
        ObjectNode input = Node.objectNode().withMember("a", Node.from("b"));
        ObjectNode output = Node.objectNode().withMember("c", Node.from("d"));
        ErrorExample errorExample1 = ErrorExample.builder()
                .shapeId(ShapeId.from("smithy.example#FooError"))
                .content(Node.objectNode()
                        .withMember("e", Node.from("f")))
                .build();
        ErrorExample errorExample2 = ErrorExample.builder()
                .shapeId(ShapeId.from("smithy.example#FooError"))
                .content(Node.objectNode()
                        .withMember("e", Node.from("f")))
                .build();
        ErrorExample errorExample3 = ErrorExample.builder()
                .shapeId(ShapeId.from("smithy.example#FooError"))
                .content(Node.objectNode()
                        .withMember("g", Node.from("h")))
                .build();
        Example example1 = Example.builder()
                .title("foo")
                .documentation("docs")
                .input(input)
                .output(output)
                .error(errorExample1)
                .build();
        Example example2 = Example.builder()
                .title("foo")
                .documentation("docs")
                .input(input)
                .output(output)
                .error(errorExample2)
                .build();
        Example example3 = Example.builder()
                .title("foo")
                .documentation("docs")
                .input(input)
                .output(output)
                .error(errorExample3)
                .build();
        assertThat(errorExample1, equalTo(errorExample2));
        assertThat(errorExample1, not(equalTo(errorExample3)));
        assertThat(example1, equalTo(example2));
        assertThat(example1, not(equalTo(example3)));
    }

}

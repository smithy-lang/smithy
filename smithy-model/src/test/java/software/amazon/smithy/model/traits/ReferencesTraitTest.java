/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class ReferencesTraitTest {
    @Test
    public void loadsTrait() {
        ShapeId id = ShapeId.from("ns.qux#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        ArrayNode values = Node.arrayNode()
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.qux#SomeShape"))
                        .withMember("ids", Node.parse("{\"a\": \"a\", \"b\": \"b\"}")))
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#OtherShape"))
                        .withMember("ids", Node.parse("{\"c\": \"c\"}"))
                        .withMember("rel", Node.from("rel")))
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#OtherShape"))
                        .withMember("ids", Node.parse("{\"c\": \"c\"}"))
                        .withMember("service", Node.from("com.foo#Baz")));
        Optional<Trait> trait = provider.createTrait(ShapeId.from("smithy.api#references"), id, values);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ReferencesTrait.class));
        ReferencesTrait referencesTrait = (ReferencesTrait) trait.get();

        assertThat(referencesTrait.getReferences(), hasSize(3));

        assertThat(referencesTrait.getReferences().get(0).getResource(),
                equalTo(ShapeId.from(id.getNamespace() + "#SomeShape")));
        assertThat(referencesTrait.getReferences().get(0).getIds(), hasKey("a"));
        assertThat(referencesTrait.getReferences().get(0).getIds(), hasKey("b"));

        assertThat(referencesTrait.getReferences().get(1).getResource(),
                equalTo(ShapeId.from("ns.foo#OtherShape")));
        assertThat(referencesTrait.getReferences().get(1).getIds(), hasKey("c"));

        assertThat(referencesTrait.getReferences().get(2).getResource(),
                equalTo(ShapeId.from("ns.foo#OtherShape")));
        assertThat(referencesTrait.getReferences().get(2).getService(),
                equalTo(Optional.of(ShapeId.from("com.foo#Baz"))));
        assertThat(referencesTrait.getReferences().get(2).getIds(), hasKey("c"));
    }

    @Test
    public void convertsToNodeAndBuilder() {
        ShapeId id = ShapeId.from("ns.qux#foo");
        ArrayNode values = Node.arrayNode()
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#SomeShape"))
                        .withMember("ids", Node.parse("{\"a\": \"a\", \"b\": \"b\"}")))
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#OtherShape"))
                        .withMember("ids", Node.parse("{\"c\": \"c\"}"))
                        .withMember("rel", Node.from("rel")))
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#OtherShape"))
                        .withMember("ids", Node.parse("{\"c\": \"c\"}"))
                        .withMember("service", Node.from("foo.baz#Bar")));
        ReferencesTrait trait = new ReferencesTrait.Provider().createTrait(id, values);

        assertThat(trait.toNode(), equalTo(values));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }
}

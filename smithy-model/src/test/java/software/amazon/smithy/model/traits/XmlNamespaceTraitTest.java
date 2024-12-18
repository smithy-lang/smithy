/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class XmlNamespaceTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("uri", Node.from("https://www.amazon.com"));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#xmlNamespace"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(XmlNamespaceTrait.class));
        XmlNamespaceTrait xmlNamespace = (XmlNamespaceTrait) trait.get();

        assertThat(xmlNamespace.getUri(), equalTo("https://www.amazon.com"));
    }

    @Test
    public void omitsEmptiesFromSerializedNode() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("uri", Node.from("https://www.amazon.com"));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#xmlNamespace"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(XmlNamespaceTrait.class));
        ObjectNode serialized = ((XmlNamespaceTrait) trait.get()).createNode().expectObjectNode();

        assertFalse(serialized.getMember("prefix").isPresent());
    }

    @Test
    public void loadsTraitWithPrefix() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("uri", Node.from("https://www.amazon.com"))
                .withMember("prefix", Node.from("xsi"));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#xmlNamespace"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(XmlNamespaceTrait.class));
        XmlNamespaceTrait xmlNamespace = (XmlNamespaceTrait) trait.get();

        assertThat(xmlNamespace.getUri(), equalTo("https://www.amazon.com"));
        assertTrue(xmlNamespace.getPrefix().isPresent());
        assertThat(xmlNamespace.getPrefix().get(), equalTo("xsi"));
    }
}

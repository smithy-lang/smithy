/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
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
        assertFalse(typedTrait.getApiConfig().isPresent());
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
        assertFalse(typedTrait.getApiConfig().isPresent());
    }

    @Test
    public void loadsTraitWithApiConfig() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode apiConfigNode = Node.objectNodeBuilder()
                .withMember("tagApi", "ns.qux#AddTagsToResource")
                .withMember("untagApi", "ns.qux#RemoveTagsFromResource")
                .withMember("listTagsApi", "ns.qux#DescribeTagsForResource")
                .build();
        ObjectNode objectNode = Node.objectNodeBuilder()
                .withMember("apiConfig", apiConfigNode)
                .build();

        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("aws.api#tagEnabled"),
                ShapeId.from("ns.qux#foo"),
                objectNode);

        assertTrue(trait.isPresent());
        TagEnabledTrait typedTrait = (TagEnabledTrait) trait.get();
        assertFalse(typedTrait.getDisableDefaultOperations());
        assertTrue(typedTrait.getApiConfig().isPresent());
        TaggableServiceApiConfig cfg = typedTrait.getApiConfig().get();
        assertEquals(Optional.of(ShapeId.from("ns.qux#AddTagsToResource")), cfg.getTagApi());
        assertEquals(Optional.of(ShapeId.from("ns.qux#RemoveTagsFromResource")), cfg.getUntagApi());
        assertEquals(Optional.of(ShapeId.from("ns.qux#DescribeTagsForResource")), cfg.getListTagsApi());
        assertThat(typedTrait.toNode(), equalTo(objectNode));
    }

    @Test
    public void loadsTraitWithPartialApiConfig() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode apiConfigNode = Node.objectNodeBuilder()
                .withMember("tagApi", "ns.qux#AddTagsToResource")
                .build();
        ObjectNode objectNode = Node.objectNodeBuilder()
                .withMember("apiConfig", apiConfigNode)
                .build();

        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("aws.api#tagEnabled"),
                ShapeId.from("ns.qux#foo"),
                objectNode);

        assertTrue(trait.isPresent());
        TagEnabledTrait typedTrait = (TagEnabledTrait) trait.get();
        TaggableServiceApiConfig cfg = typedTrait.getApiConfig().get();
        assertEquals(Optional.of(ShapeId.from("ns.qux#AddTagsToResource")), cfg.getTagApi());
        assertFalse(cfg.getUntagApi().isPresent());
        assertFalse(cfg.getListTagsApi().isPresent());
        assertThat(typedTrait.toNode(), equalTo(objectNode));
        assertThat(typedTrait.toBuilder().build(), equalTo(typedTrait));
    }
}

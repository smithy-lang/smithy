/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ObjectNode.Builder;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class TaggableTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Builder objectNodeBuilder = Node.objectNodeBuilder();
        objectNodeBuilder.withMember("property", "propertyName");
        TaggableApiConfig.Builder apiConfigNodeBuilder = TaggableApiConfig.builder();
        apiConfigNodeBuilder.tagApi(ShapeId.from("ns.qux#TagIt"));
        apiConfigNodeBuilder.untagApi(ShapeId.from("ns.qux#Untag"));
        apiConfigNodeBuilder.listTagsApi(ShapeId.from("ns.qux#ListTags"));
        TaggableApiConfig apiConfig = apiConfigNodeBuilder.build();
        objectNodeBuilder.withOptionalMember("apiConfig", Optional.of(apiConfig));
        objectNodeBuilder.withMember("disableSystemTags", true);
        ObjectNode objectNode = objectNodeBuilder.build();
        Optional<Trait> trait = provider.createTrait(ShapeId.from("aws.api#taggable"),
                ShapeId.from("ns.qux#foo"),
                objectNode);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(TaggableTrait.class));
        TaggableTrait taggableTrait = (TaggableTrait) trait.get();

        assertTrue(taggableTrait.getProperty().isPresent());
        assertEquals("propertyName", taggableTrait.getProperty().get());
        assertTrue(taggableTrait.getApiConfig().isPresent());
        assertEquals(ShapeId.from("ns.qux#TagIt"), taggableTrait.getApiConfig().get().getTagApi());
        assertEquals(ShapeId.from("ns.qux#Untag"), taggableTrait.getApiConfig().get().getUntagApi());
        assertEquals(ShapeId.from("ns.qux#ListTags"), taggableTrait.getApiConfig().get().getListTagsApi());
        assertTrue(taggableTrait.getDisableSystemTags());

        assertThat(taggableTrait.toNode(), equalTo(objectNode));
        assertThat(taggableTrait.toBuilder().build(), equalTo(taggableTrait));
    }

    @Test
    public void loadsEmptySpecificationDefaults() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode objectNode = Node.objectNode();
        Optional<Trait> trait = provider.createTrait(ShapeId.from("aws.api#taggable"),
                ShapeId.from("ns.qux#foo"),
                objectNode);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(TaggableTrait.class));
        TaggableTrait taggableTrait = (TaggableTrait) trait.get();

        assertFalse(taggableTrait.getProperty().isPresent());
        assertFalse(taggableTrait.getApiConfig().isPresent());
        assertEquals(false, taggableTrait.getDisableSystemTags());
        assertThat(taggableTrait.toNode(), equalTo(objectNode));
    }
}

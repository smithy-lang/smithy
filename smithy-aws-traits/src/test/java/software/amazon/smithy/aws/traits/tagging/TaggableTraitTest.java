/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.aws.traits.tagging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
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
        objectNodeBuilder.withMember("tagApi", "ns.qux#TagIt");
        objectNodeBuilder.withMember("untagApi", "ns.qux#Untag");
        objectNodeBuilder.withMember("listTagsApi", "ns.qux#ListTags");
        objectNodeBuilder.withMember("supportsSystemTags", false);
        ObjectNode objectNode = objectNodeBuilder.build();
        Optional<Trait> trait = provider.createTrait(ShapeId.from("aws.api#taggable"),
            ShapeId.from("ns.qux#foo"), objectNode);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(TaggableTrait.class));
        TaggableTrait taggableTrait = (TaggableTrait) trait.get();

        assertTrue(taggableTrait.getProperty().isPresent());
        assertEquals("propertyName", taggableTrait.getProperty().get());
        assertEquals(ShapeId.from("ns.qux#TagIt"), taggableTrait.getTagApi().get());
        assertEquals(ShapeId.from("ns.qux#Untag"), taggableTrait.getUntagApi().get());
        assertEquals(ShapeId.from("ns.qux#ListTags"), taggableTrait.getListTagsApi().get());
        assertFalse(taggableTrait.getSupportsSystemTags().get());

        assertThat(taggableTrait.toNode(), equalTo(objectNode));
        assertThat(taggableTrait.toBuilder().build(), equalTo(taggableTrait));
    }

    @Test
    public void loadsEmptySpecificationDefaults() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode objectNode = Node.objectNode();
        Optional<Trait> trait = provider.createTrait(ShapeId.from("aws.api#taggable"),
            ShapeId.from("ns.qux#foo"), objectNode);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(TaggableTrait.class));
        TaggableTrait taggableTrait = (TaggableTrait) trait.get();

        assertFalse(taggableTrait.getProperty().isPresent());
        assertEquals(Optional.empty(), taggableTrait.getTagApi());
        assertEquals(Optional.empty(), taggableTrait.getUntagApi());
        assertEquals(Optional.empty(), taggableTrait.getListTagsApi());
        assertEquals(Optional.empty(), taggableTrait.getSupportsSystemTags());
        assertThat(taggableTrait.toNode(), equalTo(objectNode));
        assertThat(taggableTrait.toBuilder().build(), equalTo(taggableTrait));
    }
}

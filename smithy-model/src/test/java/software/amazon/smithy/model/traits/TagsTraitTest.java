/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class TagsTraitTest {
    @Test
    public void loadsTrait() {
        Node node = Node.fromStrings("experimental");
        ShapeId id = ShapeId.from("ns.qux#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ShapeId.from("smithy.api#tags"), id, node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(TagsTrait.class));
        TagsTrait tagsTrait = (TagsTrait) trait.get();
        assertThat(tagsTrait.getValues(), contains("experimental"));
        assertThat(tagsTrait.toNode(), equalTo(node));
    }

    @Test
    public void hasSourceLocation() {
        SourceLocation location1 = new SourceLocation("/foo.smithy", 1, 1);
        SourceLocation location2 = new SourceLocation("/foo.smithy", 1, 2);
        ArrayNode node = new ArrayNode(ListUtils.of(new StringNode("a", location1)), location2);
        ShapeId id = ShapeId.from("ns.qux#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ShapeId.from("smithy.api#tags"), id, node);

        assertTrue(trait.isPresent());
        assertThat(trait.get().getSourceLocation(), equalTo(location2));
        assertThat(trait.get().toNode().getSourceLocation(), equalTo(location2));
    }
}

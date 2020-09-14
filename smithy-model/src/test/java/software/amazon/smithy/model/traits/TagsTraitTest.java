/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

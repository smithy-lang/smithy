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

package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class OfSelectorTest {
    @Test
    public void matchesMembersThatAreContainedWithinSelector() {
        ShapeIndex index = createIndex();
        // Containing shape must have the sensitive trait.
        Selector selector = new OfSelector(Collections.singletonList(
                new AttributeSelector(new TraitAttributeKey("sensitive"))));

        Set<Shape> result = selector.select(index);
        assertThat(result, hasSize(1));
        assertThat(result.iterator().next().getId().toString(), equalTo("foo.baz#B$member"));
    }

    @Test
    public void matchesMembersThatAreContainedWithinSelectorUsingOr() {
        ShapeIndex index = createIndex();
        // Match either a structure shape container or a list container.
        Selector selector = new OfSelector(Arrays.asList(
                new ShapeTypeSelector(ShapeType.STRUCTURE),
                new ShapeTypeSelector(ShapeType.LIST)));

        Set<Shape> result = selector.select(index);
        assertThat(result, containsInAnyOrder(
                index.getShape(ShapeId.from("foo.baz#B$member")).get(),
                index.getShape(ShapeId.from("foo.baz#C$member")).get(),
                index.getShape(ShapeId.from("foo.baz#D$member")).get()));
    }

    private ShapeIndex createIndex() {
        Shape a = StringShape.builder().id("foo.baz#A").build();
        MemberShape bMember = MemberShape.builder()
                .id("foo.baz#B$member")
                .target(a.getId())
                .build();
        Shape b = StructureShape.builder().id("foo.baz#B")
                .addMember(bMember)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        MemberShape cMember = MemberShape.builder()
                .id("foo.baz#C$member")
                .target(a.getId())
                .build();
        Shape c = StructureShape.builder().id("foo.baz#C")
                .addMember(cMember)
                .build();
        MemberShape dMember = MemberShape.builder()
                .id("foo.baz#D$member")
                .target(a.getId())
                .build();
        Shape d = ListShape.builder()
                .id("foo.baz#D")
                .member(dMember)
                .build();

        return ShapeIndex.builder().addShapes(a, b, bMember, c, cMember, d, dMember).build();
    }
}

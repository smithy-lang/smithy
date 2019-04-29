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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class PathFinderTest {
    @Test
    public void findsPathsFromAtoB() {
        StringShape string = StringShape.builder().id("a.b#String").build();
        MemberShape listMember = MemberShape.builder().id("a.b#List$member").target(string).build();
        ListShape list = ListShape.builder().id("a.b#List").member(listMember).build();
        MemberShape structMemberFoo = MemberShape.builder().id("a.b#Struct$foo").target(list).build();
        MemberShape structMemberBaz = MemberShape.builder().id("a.b#Struct$baz").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("a.b#Struct")
                .addMember(structMemberFoo)
                .addMember(structMemberBaz)
                .build();
        ShapeIndex index = ShapeIndex.builder()
                .addShapes(struct, structMemberFoo, structMemberBaz, list, listMember, string, structMemberBaz)
                .build();

        List<String> result1 = formatPaths(PathFinder.create(index).search(struct, "string"));
        assertThat(result1, contains(
                "[id|a.b#Struct] -[member]-> [id|a.b#Struct$baz] > [id|a.b#String]",
                "[id|a.b#Struct] -[member]-> [id|a.b#Struct$foo] > [id|a.b#List] -[member]-> [id|a.b#List$member] > [id|a.b#String]"
        ));

        List<String> result2 = formatPaths(PathFinder.create(index).search(structMemberFoo, "string"));
        assertThat(result2, contains(
                "[id|a.b#Struct$foo] > [id|a.b#List] -[member]-> [id|a.b#List$member] > [id|a.b#String]"));
    }

    private static List<String> formatPaths(List<PathFinder.Path> rels) {
        return rels.stream().map(PathFinder.Path::toString).collect(Collectors.toList());
    }

    @Test
    public void doesNotFailOnRecursion() {
        StringShape string = StringShape.builder().id("a.b#String").addTrait(new SensitiveTrait()).build();
        MemberShape listMember = MemberShape.builder().id("a.b#List$member").target("a.b#Struct").build();
        ListShape list = ListShape.builder().id("a.b#List").member(listMember).build();
        MemberShape structMemberFoo = MemberShape.builder().id("a.b#Struct$foo").target(list).build();
        MemberShape structMemberBaz = MemberShape.builder().id("a.b#Struct$baz").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("a.b#Struct")
                .addTrait(new SensitiveTrait())
                .addMember(structMemberFoo)
                .addMember(structMemberBaz)
                .build();
        ShapeIndex index = ShapeIndex.builder()
                .addShapes(struct, structMemberFoo, structMemberBaz, list, listMember, string, structMemberBaz)
                .build();
        List<String> result = formatPaths(PathFinder.create(index).search(struct, "[trait|sensitive]"));

        assertThat(result, containsInAnyOrder(
                "[id|a.b#Struct] -[member]-> [id|a.b#Struct$foo] > [id|a.b#List] -[member]-> [id|a.b#List$member] > [id|a.b#Struct]",
                "[id|a.b#Struct] -[member]-> [id|a.b#Struct$baz] > [id|a.b#String]"
        ));
    }

    @Test
    public void emptyResultsWhenNothingMatchesSelector() {
        StringShape string = StringShape.builder().id("a.b#String").build();
        ShapeIndex index = ShapeIndex.builder().addShapes(string).build();
        List<String> result = formatPaths(PathFinder.create(index).search(string, "[trait|required]"));

        assertThat(result, empty());
    }

    @Test
    public void doesNotFailOnMissingShape() {
        ShapeIndex index = ShapeIndex.builder().build();
        List<String> result = formatPaths(PathFinder.create(index).search(ShapeId.from("foo.baz#Bar"), "string"));

        assertThat(result, empty());
    }

    @Test
    public void doesNotAddItselfToResultAsFirstMatch() {
        MemberShape listMember = MemberShape.builder().id("a.b#List$member").target("a.b#List").build();
        ListShape list = ListShape.builder().id("a.b#List").member(listMember).build();
        ShapeIndex index = ShapeIndex.builder().addShapes(list, listMember).build();
        List<String> result = formatPaths(PathFinder.create(index).search(list, "list"));

        assertThat(result, contains(
                "[id|a.b#List] -[member]-> [id|a.b#List$member] > [id|a.b#List]"));
    }

    @Test
    public void providesStartAndEndShapes() {
        MemberShape listMember = MemberShape.builder().id("a.b#List$member").target("a.b#List").build();
        ListShape list = ListShape.builder().id("a.b#List").member(listMember).build();
        ShapeIndex index = ShapeIndex.builder().addShapes(list, listMember).build();
        List<PathFinder.Path> result = PathFinder.create(index).search(list, "list");

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getStartShape(), equalTo(list));
        assertThat(result.get(0).getEndShape(), equalTo(list));
        assertThat(result.get(0).size(), equalTo(2));
    }
}

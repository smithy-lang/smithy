/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
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
        Model model = Model.builder()
                .addShapes(struct, structMemberFoo, structMemberBaz, list, listMember, string, structMemberBaz)
                .build();

        List<String> result1 = formatPaths(PathFinder.create(model).search(struct, "string"));
        assertThat(result1,
                containsInAnyOrder(
                        "[id|a.b#Struct] -[member]-> [id|a.b#Struct$baz] > [id|a.b#String]",
                        "[id|a.b#Struct] -[member]-> [id|a.b#Struct$foo] > [id|a.b#List] -[member]-> [id|a.b#List$member] > [id|a.b#String]"));

        List<String> result2 = formatPaths(PathFinder.create(model).search(structMemberFoo, "string"));
        assertThat(result2,
                contains(
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
        Model model = Model.builder()
                .addShapes(struct, structMemberFoo, structMemberBaz, list, listMember, string, structMemberBaz)
                .build();
        List<String> result = formatPaths(PathFinder.create(model).search(struct, "[trait|sensitive]"));

        assertThat(result,
                containsInAnyOrder(
                        "[id|a.b#Struct] -[member]-> [id|a.b#Struct$foo] > [id|a.b#List] -[member]-> [id|a.b#List$member] > [id|a.b#Struct]",
                        "[id|a.b#Struct] -[member]-> [id|a.b#Struct$baz] > [id|a.b#String]"));
    }

    @Test
    public void doesNotFailOnMoreComplexRecursion() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("path-finder-recursion.json"))
                .assemble()
                .unwrap();
        PathFinder finder = PathFinder.create(model);
        ShapeId operation = ShapeId.from("smithy.example#Operation");

        List<PathFinder.Path> result = finder.search(operation, "[trait|deprecated]");
        List<String> resultStrings = result.stream().map(PathFinder.Path::toString).collect(Collectors.toList());

        assertThat(resultStrings,
                containsInAnyOrder(
                        "[id|smithy.example#Operation] -[input]-> [id|smithy.example#OperationInput] -[member]-> [id|smithy.example#OperationInput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$structureMap] > [id|smithy.example#MapOfStructures] -[member]-> [id|smithy.example#MapOfStructures$value]",
                        "[id|smithy.example#Operation] -[input]-> [id|smithy.example#OperationInput] -[member]-> [id|smithy.example#OperationInput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$structureMap] > [id|smithy.example#MapOfStructures] -[member]-> [id|smithy.example#MapOfStructures$value] > [id|smithy.example#SimpleStructure] -[member]-> [id|smithy.example#SimpleStructure$crackle]",
                        "[id|smithy.example#Operation] -[input]-> [id|smithy.example#OperationInput] -[member]-> [id|smithy.example#OperationInput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$structureList] > [id|smithy.example#ListOfStructures] -[member]-> [id|smithy.example#ListOfStructures$member] > [id|smithy.example#SimpleStructure] -[member]-> [id|smithy.example#SimpleStructure$crackle]",
                        "[id|smithy.example#Operation] -[input]-> [id|smithy.example#OperationInput] -[member]-> [id|smithy.example#OperationInput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$nestedStructure] > [id|smithy.example#SimpleStructure] -[member]-> [id|smithy.example#SimpleStructure$crackle]",
                        "[id|smithy.example#Operation] -[input]-> [id|smithy.example#OperationInput] -[member]-> [id|smithy.example#OperationInput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$recursiveStructure] > [id|smithy.example#RecursiveStructure] -[member]-> [id|smithy.example#RecursiveStructure$bar]",
                        "[id|smithy.example#Operation] -[output]-> [id|smithy.example#OperationOutput] -[member]-> [id|smithy.example#OperationOutput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$structureMap] > [id|smithy.example#MapOfStructures] -[member]-> [id|smithy.example#MapOfStructures$value] > [id|smithy.example#SimpleStructure] -[member]-> [id|smithy.example#SimpleStructure$crackle]",
                        "[id|smithy.example#Operation] -[output]-> [id|smithy.example#OperationOutput] -[member]-> [id|smithy.example#OperationOutput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$structureMap] > [id|smithy.example#MapOfStructures] -[member]-> [id|smithy.example#MapOfStructures$value]",
                        "[id|smithy.example#Operation] -[output]-> [id|smithy.example#OperationOutput] -[member]-> [id|smithy.example#OperationOutput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$structureList] > [id|smithy.example#ListOfStructures] -[member]-> [id|smithy.example#ListOfStructures$member] > [id|smithy.example#SimpleStructure] -[member]-> [id|smithy.example#SimpleStructure$crackle]",
                        "[id|smithy.example#Operation] -[output]-> [id|smithy.example#OperationOutput] -[member]-> [id|smithy.example#OperationOutput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$nestedStructure] > [id|smithy.example#SimpleStructure] -[member]-> [id|smithy.example#SimpleStructure$crackle]",
                        "[id|smithy.example#Operation] -[output]-> [id|smithy.example#OperationOutput] -[member]-> [id|smithy.example#OperationOutput$payload] > [id|smithy.example#ComplexStructure] -[member]-> [id|smithy.example#ComplexStructure$recursiveStructure] > [id|smithy.example#RecursiveStructure] -[member]-> [id|smithy.example#RecursiveStructure$bar]"));
    }

    @Test
    public void emptyResultsWhenNothingMatchesSelector() {
        StringShape string = StringShape.builder().id("a.b#String").build();
        Model model = Model.builder().addShapes(string).build();
        List<String> result = formatPaths(PathFinder.create(model).search(string, "[trait|required]"));

        assertThat(result, empty());
    }

    @Test
    public void doesNotFailOnMissingShape() {
        Model model = Model.builder().build();
        List<String> result = formatPaths(PathFinder.create(model).search(ShapeId.from("foo.baz#Bar"), "string"));

        assertThat(result, empty());
    }

    @Test
    public void doesNotAddItselfToResultAsFirstMatch() {
        MemberShape listMember = MemberShape.builder().id("a.b#List$member").target("a.b#List").build();
        ListShape list = ListShape.builder().id("a.b#List").member(listMember).build();
        Model model = Model.builder().addShapes(list, listMember).build();
        List<String> result = formatPaths(PathFinder.create(model).search(list, "list"));

        assertThat(result,
                contains(
                        "[id|a.b#List] -[member]-> [id|a.b#List$member] > [id|a.b#List]"));
    }

    @Test
    public void providesStartAndEndShapes() {
        MemberShape listMember = MemberShape.builder().id("a.b#List$member").target("a.b#List").build();
        ListShape list = ListShape.builder().id("a.b#List").member(listMember).build();
        Model model = Model.builder().addShapes(list, listMember).build();
        List<PathFinder.Path> result = PathFinder.create(model).search(list, "list");

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getStartShape(), equalTo(list));
        assertThat(result.get(0).getEndShape(), equalTo(list));
        assertThat(result.get(0).size(), equalTo(2));
    }

    @Test
    public void createsPathToInputAndOutputMember() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("path-to-member.smithy"))
                .assemble()
                .unwrap();
        PathFinder finder = PathFinder.create(model);
        ShapeId operation = ShapeId.from("smithy.example#Operation");

        Optional<PathFinder.Path> input = finder.createPathToInputMember(operation, "foo");
        assertThat(input.isPresent(), is(true));
        assertThat(input.get().toString(),
                equalTo("[id|smithy.example#Operation] -[input]-> [id|smithy.example#Input] -[member]-> [id|smithy.example#Input$foo] > [id|smithy.api#String]"));

        Optional<PathFinder.Path> output = finder.createPathToOutputMember(operation, "foo");
        assertThat(output.isPresent(), is(true));
        assertThat(output.get().toString(),
                equalTo("[id|smithy.example#Operation] -[output]-> [id|smithy.example#Output] -[member]-> [id|smithy.example#Output$foo] > [id|smithy.api#String]"));
    }

    @Test
    public void createsPathToOtherShape() {
        MemberShape recursiveMember = MemberShape.builder().id("a.b#Struct$a").target("a.b#Struct").build();
        StructureShape struct = StructureShape.builder()
                .id("a.b#Struct")
                .addMember(recursiveMember)
                .build();
        Model model = Model.builder().addShapes(struct).build();
        PathFinder finder = PathFinder.create(model);
        List<PathFinder.Path> paths = finder.search(struct.getId(), Collections.singleton(struct));

        assertThat(paths, hasSize(1));
        assertThat(paths.get(0), hasSize(2));
        assertThat(paths.get(0).getShapes(), hasSize(3));
        assertThat(paths.get(0).getStartShape(), equalTo(struct));
        assertThat(paths.get(0).getEndShape(), equalTo(struct));
    }
}

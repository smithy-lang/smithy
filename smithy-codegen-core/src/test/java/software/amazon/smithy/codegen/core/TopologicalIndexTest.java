/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.FunctionalUtils;

public class TopologicalIndexTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(TopologicalIndexTest.class.getResource("topological-sort.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void sortsTopologically() {
        TopologicalIndex index = TopologicalIndex.of(model);

        List<String> ordered = new ArrayList<>();
        for (Shape shape : index.getOrderedShapes()) {
            ordered.add(shape.getId().toString());
        }

        List<String> recursive = new ArrayList<>();
        for (Shape shape : index.getRecursiveShapes()) {
            recursive.add(shape.getId().toString());
        }

        assertThat(ordered,
                contains(
                        "smithy.example#MyString",
                        "smithy.example#BamList$member",
                        "smithy.example#BamList",
                        "smithy.example#Bar$bam",
                        "smithy.api#Integer",
                        "smithy.example#Bar$baz",
                        "smithy.example#Bar",
                        "smithy.example#Foo$bar",
                        "smithy.example#Foo$foo",
                        "smithy.example#Foo"));

        assertThat(recursive,
                contains(
                        "smithy.example#Recursive",
                        "smithy.example#Recursive$b",
                        "smithy.example#RecursiveList$member",
                        "smithy.example#RecursiveList",
                        "smithy.example#Recursive$a"));
    }

    @Test
    public void checksIfShapeByIdIsRecursive() {
        TopologicalIndex index = TopologicalIndex.of(model);

        assertThat(index.isRecursive(ShapeId.from("smithy.example#Recursive$b")), is(true));
        assertThat(index.isRecursive(ShapeId.from("smithy.example#MyString")), is(false));
    }

    @Test
    public void checksIfShapeIsRecursive() {
        TopologicalIndex index = TopologicalIndex.of(model);

        assertThat(index.isRecursive(model.expectShape(ShapeId.from("smithy.example#MyString"))), is(false));
        assertThat(index.isRecursive(model.expectShape(ShapeId.from("smithy.example#Recursive$b"))), is(true));
    }

    @Test
    public void getsRecursiveClosureById() {
        TopologicalIndex index = TopologicalIndex.of(model);

        assertThat(index.getRecursiveClosure(ShapeId.from("smithy.example#MyString")), empty());
        assertThat(index.getRecursiveClosure(ShapeId.from("smithy.example#Recursive$b")), not(empty()));
    }

    @Test
    public void getsRecursiveClosureByShape() {
        TopologicalIndex index = TopologicalIndex.of(model);

        assertThat(index.getRecursiveClosure(model.expectShape(ShapeId.from("smithy.example#MyString"))),
                empty());
        assertThat(index.getRecursiveClosure(model.expectShape(ShapeId.from("smithy.example#Recursive$b"))),
                not(empty()));
    }

    @Test
    public void handlesMoreRecursion() {
        Model recursive = Model.assembler()
                .addImport(getClass().getResource("topological-recursion.smithy"))
                .assemble()
                .unwrap();
        TopologicalIndex index = TopologicalIndex.of(recursive);

        // The topological index must capture all shapes in the index not in the prelude.
        Set<Shape> nonPrelude = recursive.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .collect(Collectors.toSet());
        Set<Shape> topologicalShapes = new HashSet<>(index.getOrderedShapes());
        topologicalShapes.addAll(index.getRecursiveShapes());
        assertThat(topologicalShapes, equalTo(nonPrelude));

        // The ordered shape IDs must be in the this order.
        List<String> orderedIds = index.getOrderedShapes()
                .stream()
                .map(Shape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toList());
        assertThat(orderedIds,
                contains(
                        "smithy.example#MyString",
                        "smithy.example#NonRecursive$foo",
                        "smithy.example#NonRecursive",
                        "smithy.example#NonRecursiveList$member",
                        "smithy.example#NonRecursiveList",
                        "smithy.example#User$notRecursive",
                        "smithy.example#UsersMap$key"));

        List<String> recursiveIds = index.getRecursiveShapes()
                .stream()
                .map(Shape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toList());
        assertThat(recursiveIds,
                contains(
                        "smithy.example#User",
                        "smithy.example#User$recursiveUser",
                        "smithy.example#UsersList$member",
                        "smithy.example#UsersMap$value",
                        "smithy.example#GetFooInput$foo",
                        "smithy.example#UsersList",
                        "smithy.example#UsersMap",
                        "smithy.example#GetFooInput",
                        "smithy.example#User$recursiveList",
                        "smithy.example#User$recursiveMap",
                        "smithy.example#GetFoo",
                        "smithy.example#Example"));

        for (String recursiveId : recursiveIds) {
            ShapeId id = ShapeId.from(recursiveId);
            // Must be considered recursive.
            assertThat(index.isRecursive(id), is(true));
            // Must not be in the ordered set.
            assertThat(index.getOrderedShapes().contains(recursive.expectShape(id)), is(false));
            // Must not have an empty recursion closure.
            assertThat(index.getRecursiveClosure(id), not(empty()));
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ShapeClosureIndexTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(ShapeClosureIndexTest.class.getResource("shape-closure-index-test.smithy"))
                .addImport(ShapeClosureIndexTest.class.getResource("shape-closure-index-test-other.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void includesShapesByNamespaceAndWalksReferences() {
        ShapeClosureIndex index = ShapeClosureIndex.of(model);

        Set<ShapeId> ids = index.getShapesInClosure("com.example#Namespaced")
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());

        assertThat(ids, hasItem(ShapeId.from("com.example#Foo")));
        assertThat(ids, hasItem(ShapeId.from("com.example#Bar")));
        // Walked transitively from `Foo.other`.
        assertThat(ids, hasItem(ShapeId.from("com.other#Other")));
    }

    @Test
    public void rootShapesDontIncludeReferences() {
        ShapeClosureIndex index = ShapeClosureIndex.of(model);

        Set<ShapeId> ids = index.getRootShapesInClosure("com.example#Namespaced")
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());

        assertThat(ids, hasItem(ShapeId.from("com.example#Foo")));
        assertThat(ids, hasItem(ShapeId.from("com.example#Bar")));
        // Walked transitively from `Foo.other`.
        assertThat(ids, not(hasItem(ShapeId.from("com.other#Other"))));
    }

    @Test
    public void includesShapesBySelector() {
        ShapeClosureIndex index = ShapeClosureIndex.of(model);

        Set<ShapeId> ids = index.getShapesInClosure("com.example#BySelector")
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());

        assertThat(ids, hasItem(ShapeId.from("com.other#Other")));
    }

    @Test
    public void walksOnlyDirectedRelationships() {
        Model directedModel = Model.assembler()
                .addImport(ShapeClosureIndexTest.class.getResource("shape-closure-index-directed-test.smithy"))
                .assemble()
                .unwrap();
        ShapeClosureIndex index = ShapeClosureIndex.of(directedModel);

        Set<ShapeId> ids = index.getShapesInClosure("com.example#operations")
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());

        // Descends into the operation's input.
        assertThat(ids, hasItem(ShapeId.from("com.example#GetCity")));
        assertThat(ids, hasItem(ShapeId.from("com.example#GetCityInput")));
        // Does not crawl up to the service that binds the operation.
        assertThat(ids, not(hasItem(ShapeId.from("com.example#Weather"))));
    }

    @Test
    public void exposesRenames() {
        ShapeClosureIndex index = ShapeClosureIndex.of(model);

        assertThat(index.getRenames("com.example#Renamed").get(ShapeId.from("com.example#Foo")),
                equalTo("RenamedFoo"));
        assertThat(index.getRenames("com.example#Namespaced").entrySet(), empty());
    }

    @Test
    public void listsClosureIds() {
        ShapeClosureIndex index = ShapeClosureIndex.of(model);

        assertThat(index.getClosureIds(),
                containsInAnyOrder(
                        "com.example#Namespaced",
                        "com.example#BySelector",
                        "com.example#Renamed"));
    }

    @Test
    public void throwsOnUnknownClosure() {
        ShapeClosureIndex index = ShapeClosureIndex.of(model);

        assertThrows(ExpectationNotMetException.class, () -> index.getShapesInClosure("com.example#Missing"));
        assertThrows(ExpectationNotMetException.class, () -> index.getRenames("com.example#Missing"));
    }

    @Test
    public void worksWhenNoClosuresAreDeclared() {
        Model empty = Model.assembler().assemble().unwrap();
        ShapeClosureIndex index = ShapeClosureIndex.of(empty);

        assertThat(index.getClosureIds(), empty());
        assertThrows(ExpectationNotMetException.class, () -> index.getShapesInClosure("any"));
    }
}

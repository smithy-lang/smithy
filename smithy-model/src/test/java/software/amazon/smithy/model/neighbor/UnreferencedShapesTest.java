/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.PrivateTrait;

public class UnreferencedShapesTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(UnreferencedShapesTest.class.getResource("unreferenced-test.json"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void doesNotCheckPrelude() {
        Model model = Model.assembler().assemble().unwrap();

        assertThat(new UnreferencedShapes().compute(model), empty());
    }

    @Test
    public void doesNotCheckShapesThatAreTraitShapes() {
        UnreferencedShapes unref = new UnreferencedShapes();

        assertThat(unref.compute(model).stream().map(Shape::getId).collect(Collectors.toSet()),
                containsInAnyOrder(
                        ShapeId.from("ns.foo#Exclude1"),
                        ShapeId.from("ns.foo#Exclude2")));
    }

    @Test
    public void doesNotIgnorePrivateShapes() {
        ShapeId id = ShapeId.from("foo.baz#Bar");
        Model model = createPrivateShapeModel(id);
        UnreferencedShapes unref = new UnreferencedShapes();
        Set<ShapeId> ids = unref.compute(model).stream().map(Shape::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(id));
    }

    private Model createPrivateShapeModel(ShapeId id) {
        return Model.assembler()
                .addShape(StringShape.builder().id(id).addTrait(new PrivateTrait()).build())
                .assemble()
                .unwrap();
    }

    @Test
    public void checksShapeReferencesThroughIdRef() {
        Model m = Model.assembler()
                .addImport(getClass().getResource("idref-neighbors.smithy"))
                .assemble()
                .unwrap();

        Set<Shape> shapes = new UnreferencedShapes().compute(m);

        assertThat(shapes, empty());
    }

    @Test
    public void doesNotCheckShapeReferencesThroughIdRefOnUnconnectedShapes() {
        Model m = Model.assembler()
                .addImport(getClass().getResource("idref-neighbors-unconnected.smithy"))
                .assemble()
                .unwrap();

        Set<ShapeId> ids = new UnreferencedShapes().compute(m).stream().map(Shape::getId).collect(Collectors.toSet());

        assertThat(ids,
                containsInAnyOrder(
                        ShapeId.from("com.foo#WithTrait"),
                        ShapeId.from("com.foo#Referenced"),
                        ShapeId.from("com.foo#Unconnected")));
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.FunctionalUtils;

public class RemoveUnusedShapesTest {

    @Test
    public void treeShakerWithExports() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("tree-shaker.json").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("exportTagged", Node.fromStrings("export")))
                .build();
        Model result = new RemoveUnusedShapes().transform(context);
        List<String> ids = result.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .map(Shape::getId)
                .map(Object::toString)
                .collect(Collectors.toList());

        assertThat(ids, containsInAnyOrder("ns.foo#MyService", "ns.foo#Include1", "ns.foo#Include2"));
    }

    @Test
    public void shouldRetainUsedTraitsAndShapesUsedBySaidTraits() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("tree-shaking-traits.json").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("exportTagged", Node.arrayNode()))
                .build();
        Model result = new RemoveUnusedShapes().transform(context);

        assertTrue(result.getTraitDefinition(ShapeId.from("ns.foo#bar")).isPresent());
        assertTrue(result.getShape(ShapeId.from("ns.foo#bar")).isPresent());
        assertTrue(result.getShape(ShapeId.from("ns.foo#BarTraitShapeMember")).isPresent());
        assertFalse(result.getTraitDefinition(ShapeId.from("ns.foo#QuuxTraitShapeMember")).isPresent());
    }

    @Test
    public void shouldPruneUnusedTraitsAndShapesUsedBySaidTraits() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("tree-shaking-traits.json").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("exports", Node.arrayNode()))
                .build();
        Model result = new RemoveUnusedShapes().transform(context);

        assertFalse(result.getTraitDefinition(ShapeId.from("ns.foo#quux")).isPresent());
        assertFalse(result.getShape(ShapeId.from("ns.foo#QuuxTraitShape")).isPresent());
    }
}

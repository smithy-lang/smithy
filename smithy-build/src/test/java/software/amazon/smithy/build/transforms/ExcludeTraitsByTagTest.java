/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.TraitDefinition;

public class ExcludeTraitsByTagTest {
    @Test
    public void removesTraitsByTagInList() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("tree-shaking-traits.json").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("tags", Node.fromStrings("qux")))
                .build();
        Model result = new ExcludeTraitsByTag().transform(context);
        Set<ShapeId> traits = result.getShapesWithTrait(TraitDefinition.class)
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());

        assertFalse(traits.contains(ShapeId.from("ns.foo#quux")));
        assertTrue(traits.contains(ShapeId.from("ns.foo#bar")));

        // Mixin members are retained, but tagged traits are excluded.
        MemberShape mixedMember = result.expectShape(ShapeId.from("ns.foo#MyOperationInput$mixedMember"),
                MemberShape.class);
        assertFalse(mixedMember.findMemberTrait(result, "ns.foo#corge").isPresent());
        assertTrue(mixedMember.findMemberTrait(result, "ns.foo#bar").isPresent());
    }

    @Test
    public void classesWithMixinsFilteredWithoutCycleError() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("mixin-cycle-test.smithy").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("tags", Node.fromStrings("filter")))
                .build();
        Model result = new ExcludeTraitsByTag().transform(context);

        Set<ShapeId> traits = result.getShapesWithTrait(TraitDefinition.class)
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());

        assertFalse(traits.contains(ShapeId.from("smithy.example#filteredTrait")));
        assertTrue(traits.contains(ShapeId.from("smithy.example#unfilteredTrait")));

        ResourceShape resourceShape =
                result.expectShape(ShapeId.from("smithy.example#ResourceWithMixin"), ResourceShape.class);
        assertFalse(resourceShape.findMemberTrait(result, "smithy.example#filteredTrait").isPresent());
        assertTrue(resourceShape.findMemberTrait(result, "smithy.example#unfilteredTrait").isPresent());

        OperationShape operationShape =
                result.expectShape(ShapeId.from("smithy.example#OperationWithMixin"), OperationShape.class);
        assertFalse(operationShape.findMemberTrait(result, "smithy.example#filteredTrait").isPresent());
        assertTrue(operationShape.findMemberTrait(result, "smithy.example#unfilteredTrait").isPresent());

        StructureShape structureShape =
                result.expectShape(ShapeId.from("smithy.example#StructureWithMixin"), StructureShape.class);
        assertFalse(structureShape.findMemberTrait(result, "smithy.example#filteredTrait").isPresent());
        assertTrue(structureShape.findMemberTrait(result, "smithy.example#unfilteredTrait").isPresent());

        UnionShape unionShape = result.expectShape(ShapeId.from("smithy.example#UnionWithMixin"), UnionShape.class);
        assertFalse(unionShape.findMemberTrait(result, "smithy.example#filteredTrait").isPresent());
        assertTrue(unionShape.findMemberTrait(result, "smithy.example#unfilteredTrait").isPresent());

        MapShape mapShape = result.expectShape(ShapeId.from("smithy.example#MapWithMixin"), MapShape.class);
        assertFalse(mapShape.findMemberTrait(result, "smithy.example#filteredTrait").isPresent());
        assertTrue(mapShape.findMemberTrait(result, "smithy.example#unfilteredTrait").isPresent());

        ListShape listShape = result.expectShape(ShapeId.from("smithy.example#ListWithMixin"), ListShape.class);
        assertFalse(listShape.findMemberTrait(result, "smithy.example#filteredTrait").isPresent());
        assertTrue(listShape.findMemberTrait(result, "smithy.example#unfilteredTrait").isPresent());

        StringShape stringShape = result.expectShape(ShapeId.from("smithy.example#StringWithMixin"), StringShape.class);
        assertFalse(stringShape.findMemberTrait(result, "smithy.example#filteredTrait").isPresent());
        assertTrue(stringShape.findMemberTrait(result, "smithy.example#unfilteredTrait").isPresent());
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.TagsTrait;

public class ExcludeShapesByTagTest {

    @Test
    public void removesTraitsNotInList() {
        StringShape stringA = StringShape.builder()
                .id("ns.foo#baz")
                .addTrait(TagsTrait.builder().addValue("foo").addValue("baz").build())
                .build();
        StringShape stringB = StringShape.builder()
                .id("ns.foo#bar")
                .addTrait(TagsTrait.builder().addValue("qux").build())
                .build();
        Model model = Model.builder()
                .addShapes(stringA, stringB)
                .build();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("tags", Node.fromStrings("foo")))
                .build();
        Model result = new ExcludeShapesByTag().transform(context);

        assertThat(result.getShape(stringA.getId()), is(Optional.empty()));
        assertThat(result.getShape(stringB.getId()), not(Optional.empty()));
    }

    @Test
    public void filtersMembers() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("filter-by-tags.smithy").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("tags", Node.fromStrings("filter")))
                .build();
        Model result = new ExcludeShapesByTag().transform(context);

        EnumShape foo = result.expectShape(ShapeId.from("smithy.example#Foo"), EnumShape.class);
        assertThat(foo.members().size(), is(1));

        IntEnumShape bar = result.expectShape(ShapeId.from("smithy.example#Bar"), IntEnumShape.class);
        assertThat(bar.members().size(), is(1));

        // Mixin members are retained, but excluded traits are removed.
        MemberShape baz = result.expectShape(ShapeId.from("smithy.example#StructForMixin$baz"), MemberShape.class);
        assertFalse(baz.findMemberTrait(result, "MyTrait").isPresent());
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
        Model result = new ExcludeShapesByTag().transform(context);

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

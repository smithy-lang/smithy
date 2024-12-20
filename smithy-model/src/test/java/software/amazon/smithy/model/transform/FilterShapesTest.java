/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TraitDefinition;

public class FilterShapesTest {

    @Test
    public void removesShapesThatMatchPredicate() {
        ShapeId aId = ShapeId.from("ns.foo#A");
        StringShape a = StringShape.builder()
                .id(aId)
                .addTrait(new SensitiveTrait())
                .build();
        ShapeId bId = ShapeId.from("ns.foo#B");
        StringShape b = StringShape.builder().id(bId).build();
        Model model = Model.builder().addShapes(a, b).build();
        Model result = ModelTransformer.create()
                .filterShapes(model, shape -> !shape.getTrait(SensitiveTrait.class).isPresent());

        assertThat(result.shapes().count(), Matchers.is(1L));
        assertThat(result.getShape(bId), Matchers.not(Optional.empty()));
    }

    @Test
    public void doesNotFilterListOrMapMembers() {
        ShapeId stringShapeId = ShapeId.from("ns.foo#StringShape");
        StringShape string = StringShape.builder().id(stringShapeId).build();

        MemberShape listMember = MemberShape.builder().id("ns.foo#List$member").target(stringShapeId).build();
        ListShape list = ListShape.builder().id("ns.foo#List").member(listMember).build();

        MemberShape mapKey = MemberShape.builder().id("ns.foo#Map$key").target(stringShapeId).build();
        MemberShape mapValue = MemberShape.builder().id("ns.foo#Map$value").target(stringShapeId).build();
        MapShape map = MapShape.builder().id("ns.foo#Map").key(mapKey).value(mapValue).build();

        Model model = Model.builder()
                .addShapes(string, list, listMember, map, mapKey, mapValue)
                .build();
        Model result = ModelTransformer.create().filterShapes(model, shape -> !shape.isMemberShape());

        // No members should be removed because they are not eligible.
        assertThat(result.shapes().count(), Matchers.is(6L));
    }

    @Test
    public void updatesStructureContainerWhenMemberIsRemoved() {
        ShapeId stringShapeId = ShapeId.from("ns.foo#String");
        StringShape string = StringShape.builder().id(stringShapeId).build();

        MemberShape member1 = MemberShape.builder().id("ns.foo#Structure$member1").target("ns.foo#String").build();
        MemberShape member2 = MemberShape.builder().id("ns.foo#Structure$member2").target("ns.foo#String").build();
        MemberShape member3 = MemberShape.builder().id("ns.foo#Structure$member3").target("ns.foo#String").build();
        StructureShape structure = StructureShape.builder()
                .id("ns.foo#Structure")
                .addMember(member1)
                .addMember(member2)
                .addMember(member3)
                .build();
        Model model = Model.builder()
                .addShapes(string, structure, member1, member2, member3)
                .build();

        // Remove "member2" from the structure.
        Model result = ModelTransformer.create().filterShapes(model, shape -> {
            return !shape.getId().toString().equals("ns.foo#Structure$member2");
        });

        // Ensure that the member shapes were removed from the index.
        assertThat(result.getShape(member1.getId()), Matchers.equalTo(Optional.of(member1)));
        assertThat(result.getShape(member3.getId()), Matchers.equalTo(Optional.of(member3)));
        assertThat(result.getShape(member2.getId()), Matchers.is(Optional.empty()));
        assertThat(result.getShape(structure.getId()), Matchers.not(Optional.empty()));

        // Make sure the structure was updated so that it no longer has the removed member shape.
        assertThat(result.getShape(structure.getId()).get().asStructureShape().get().getMember("member1"),
                Matchers.not(Optional.empty()));
        assertThat(result.getShape(structure.getId()).get().asStructureShape().get().getMember("member3"),
                Matchers.not(Optional.empty()));
        assertThat(result.getShape(structure.getId()).get().asStructureShape().get().getMember("member2"),
                Matchers.is(Optional.empty()));
    }

    @Test
    public void removesTraitsWhenDefinitionIsRemoved() {
        StringShape bazTrait = StringShape.builder()
                .id("ns.foo#baz")
                .addTrait(TraitDefinition.builder().build())
                .build();
        StringShape barTrait = StringShape.builder()
                .id("ns.foo#bar")
                .addTrait(TraitDefinition.builder().build())
                .build();

        ShapeId shapeId1 = ShapeId.from("ns.foo#id1");
        StringShape shape1 = StringShape.builder()
                .id(shapeId1)
                .addTrait(new DynamicTrait(ShapeId.from("foo.baz#foo"), Node.from(true)))
                .addTrait(new SensitiveTrait())
                .build();
        ShapeId shapeId2 = ShapeId.from("ns.foo#id2");
        StringShape shape2 = StringShape.builder()
                .id(shapeId2)
                .addTrait(new DynamicTrait(ShapeId.from("ns.foo#baz"), Node.from(true)))
                .addTrait(new DynamicTrait(ShapeId.from("ns.foo#bar"), Node.from(true)))
                .addTrait(new SensitiveTrait())
                .build();
        Model model = Model.builder()
                .addShapes(shape1, shape2, bazTrait, barTrait)
                .build();

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.filterShapes(model, shape -> !shape.getId().toString().equals("ns.foo#baz"));

        Map<Shape, TraitDefinition> definitions = new HashMap<>();
        for (Shape shape : result.getShapesWithTrait(TraitDefinition.class)) {
            definitions.put(shape, shape.expectTrait(TraitDefinition.class));
        }

        assertThat(definitions.size(), Matchers.is(1));
        assertThat(definitions, Matchers.hasKey(barTrait));
        assertThat(definitions, Matchers.hasValue(barTrait.getTrait(TraitDefinition.class).get()));

        assertThat(result.getShape(shapeId1).get().getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        assertThat(result.getShape(shapeId1).get().findTrait("ns.foo#baz"), Matchers.is(Optional.empty()));
        assertThat(result.getShape(shapeId2).get().getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        assertThat(result.getShape(shapeId2).get().findTrait("ns.foo#baz"), Matchers.is(Optional.empty()));
        assertThat(result.getShape(shapeId2).get().findTrait("ns.foo#bar"), Matchers.not(Optional.empty()));
    }
}

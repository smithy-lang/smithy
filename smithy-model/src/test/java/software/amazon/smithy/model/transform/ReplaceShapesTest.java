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

package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class ReplaceShapesTest {

    @Test
    public void cannotChangeShapeTypes() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            ShapeId shapeId = ShapeId.from("ns.foo#id1");
            StringShape shape = StringShape.builder().id(shapeId).build();
            Model model = Model.builder().addShape(shape).build();
            ModelTransformer transformer = ModelTransformer.create();
            transformer.mapShapes(model, s -> IntegerShape.builder().id(shapeId).build());
        });
    }

    @Test
    public void updatesListMemberWhenContainerUpdated() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberId = ShapeId.from("ns.foo#Container$member");
        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape member = MemberShape.builder().id(memberId).target(stringId).build();
        ListShape container = ListShape.builder().id(containerId).member(member).build();
        Model model = Model.builder()
                .addShapes(target, member, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        MemberShape newMember = MemberShape.builder()
                .id(memberId)
                .target(stringId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        ListShape newList = ListShape.builder().id(containerId).member(newMember).build();
        Model result = transformer.replaceShapes(model, Arrays.asList(newList));

        assertThat(result.shapes().count(), Matchers.equalTo(3L));
        assertThat(result.expectShape(memberId).getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(containerId), Matchers.is(newList));
        assertThat(result.expectShape(containerId, ListShape.class).getMember(), Matchers.is(newMember));
    }

    @Test
    public void updatesMapMembersWhenContainerUpdated() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId keyId = ShapeId.from("ns.foo#Container$key");
        ShapeId valueId = ShapeId.from("ns.foo#Container$value");

        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape keyMember = MemberShape.builder().id(keyId).target(stringId).build();
        MemberShape valueMember = MemberShape.builder().id(valueId).target(stringId).build();
        MapShape container = MapShape.builder().id(containerId).key(keyMember).value(valueMember).build();
        Model model = Model.builder()
                .addShapes(target, keyMember, valueMember, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();

        MemberShape newKey = MemberShape.builder()
                .id(keyId)
                .target(stringId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        MemberShape newValue = MemberShape.builder()
                .id(valueId)
                .target(stringId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        MapShape newMap = MapShape.builder().id(containerId).key(newKey).value(newValue).build();
        Model result = transformer.replaceShapes(model, Arrays.asList(newMap));

        assertThat(result.shapes().count(), Matchers.equalTo(4L));
        assertThat(result.expectShape(keyId).getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(valueId).getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(containerId), Matchers.is(newMap));
        assertThat(result.expectShape(containerId, MapShape.class).getKey(), Matchers.is(newKey));
        assertThat(result.expectShape(containerId, MapShape.class).getValue(), Matchers.is(newValue));
    }

    @Test
    public void updatesTaggedUnionMembersWhenContainerUpdated() {
        ShapeId stringShapeId = ShapeId.from("ns.foo#String");
        StringShape string = StringShape.builder().id(stringShapeId).build();

        MemberShape member1 = MemberShape.builder().id("ns.foo#Shape$member1").target("ns.foo#String").build();
        MemberShape member2 = MemberShape.builder().id("ns.foo#Shape$member2").target("ns.foo#String").build();
        MemberShape member3 = MemberShape.builder().id("ns.foo#Shape$member3").target("ns.foo#String").build();
        UnionShape shape = UnionShape.builder()
                .id("ns.foo#Shape").addMember(member1).addMember(member2).addMember(member3).build();
        Model model = Model.builder().addShapes(string, shape, member1, member2, member3).build();

        // Add a trait to a replaced member3.
        MemberShape newMember3 = MemberShape.builder()
                .id("ns.foo#Shape$member3")
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .target("ns.foo#String")
                .build();
        // Replace the union with a shape that has the new member3.
        // Also remove member2.
        UnionShape other = UnionShape.builder()
                .id("ns.foo#Shape").addMember(member1).addMember(newMember3).build();

        Model result = ModelTransformer.create().replaceShapes(model, Collections.singleton(other));

        assertThat(result.getShape(member1.getId()), Matchers.equalTo(Optional.of(member1)));
        assertThat(result.getShape(member2.getId()), Matchers.is(Optional.empty()));
        assertThat(result.getShape(member3.getId()), Matchers.equalTo(Optional.of(newMember3)));
        assertThat(result.getShape(shape.getId()), Matchers.not(Optional.empty()));
    }

    @Test
    public void updatesListShapeWhenMemberModified() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberId = ShapeId.from("ns.foo#Container$member");
        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape member = MemberShape.builder().id(memberId).target(stringId).build();
        ListShape container = ListShape.builder().id(containerId).member(member).build();
        Model model = Model.builder()
                .addShapes(target, member, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        MemberShape newMember = MemberShape.builder()
                .id(memberId)
                .target(stringId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        Model result = transformer.replaceShapes(model, Arrays.asList(newMember));

        assertThat(result.expectShape(memberId).getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(containerId, ListShape.class).getMember(), Matchers.is(newMember));
    }

    @Test
    public void updatesMapShapeWhenKeyOrValueIsUpdated() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId keyMemberId = ShapeId.from("ns.foo#Container$key");
        ShapeId valueMemberId = ShapeId.from("ns.foo#Container$value");
        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape keyMember = MemberShape.builder().id(keyMemberId).target(stringId).build();
        MemberShape valueMember = MemberShape.builder().id(valueMemberId).target(stringId).build();
        MapShape container = MapShape.builder().id(containerId).key(keyMember).value(valueMember).build();
        Model model = Model.builder()
                .addShapes(target, keyMember, valueMember, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        MemberShape newKeyMember = MemberShape.builder()
                .id(keyMemberId)
                .target(stringId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        Model resultWithNewKey = transformer.replaceShapes(model, Arrays.asList(newKeyMember));
        MemberShape newValueMember = MemberShape.builder()
                .id(valueMemberId)
                .target(stringId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        Model resultWithNewValue = transformer.replaceShapes(model, Arrays.asList(newValueMember));

        assertThat(resultWithNewKey.expectShape(keyMemberId).getTrait(SensitiveTrait.class),
                          Matchers.not(Optional.empty()));
        assertThat(resultWithNewKey.expectShape(containerId, MapShape.class).getKey(),
                          Matchers.is(newKeyMember));

        assertThat(resultWithNewValue.expectShape(valueMemberId).getTrait(SensitiveTrait.class),
                          Matchers.not(Optional.empty()));
        assertThat(resultWithNewValue.expectShape(containerId, MapShape.class).getValue(),
                          Matchers.is(newValueMember));
    }

    @Test
    public void updatesStructureWhenMemberChanges() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberAId = ShapeId.from("ns.foo#Container$a");
        ShapeId memberBId = ShapeId.from("ns.foo#Container$b");
        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape memberA = MemberShape.builder()
                .id(memberAId)
                .target(stringId)
                .addTrait(new RequiredTrait(SourceLocation.NONE))
                .build();
        MemberShape memberB = MemberShape.builder().id(memberBId).target(stringId).build();
        StructureShape container = StructureShape.builder()
                .id(containerId)
                .addMember(memberA)
                .addMember(memberB)
                .build();
        Model model = Model.builder()
                .addShapes(target, memberA, memberB, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        MemberShape newMemberB = MemberShape.builder()
                .id(memberBId)
                .target(stringId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        Model result = transformer.replaceShapes(model, Arrays.asList(newMemberB));

        // Make sure the member has the trait that was applied.
        assertThat(result.expectShape(memberBId).getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        // Make sure it's still optional.
        assertTrue(result.expectShape(containerId, StructureShape.class).getMember("b").get().isOptional());
        // Ensure that the structure that contains the shape was updated.
        assertThat(result.expectShape(containerId, StructureShape.class).getMember("b").get(),
                   Matchers.is(newMemberB));
    }

    @Test
    public void canReplaceMultipleMembersOfSameShape() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberAId = ShapeId.from("ns.foo#Container$a");
        ShapeId memberBId = ShapeId.from("ns.foo#Container$b");
        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape memberA = MemberShape.builder().id(memberAId).target(stringId).build();
        MemberShape memberB = MemberShape.builder().id(memberBId).target(stringId).build();
        StructureShape container = StructureShape.builder()
                .id(containerId)
                .addMember(memberA)
                .addMember(memberB)
                .build();
        Model model = Model.builder()
                .addShapes(target, memberA, memberB, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        MemberShape newMemberA = memberA.toBuilder().addTrait(new RequiredTrait(SourceLocation.NONE)).build();
        MemberShape newMemberB = memberB.toBuilder().addTrait(new RequiredTrait(SourceLocation.NONE)).build();
        Model result = transformer.replaceShapes(model, Arrays.asList(newMemberA, newMemberB));

        assertThat(result.expectShape(memberAId).getTrait(RequiredTrait.class), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(memberBId).getTrait(RequiredTrait.class), Matchers.not(Optional.empty()));

        // Make sure the members got updated inside of the container.
        assertTrue(result.expectShape(containerId, StructureShape.class)
                .getMember("a").get()
                .hasTrait(RequiredTrait.class));
        assertTrue(result.expectShape(containerId, StructureShape.class)
                .getMember("b").get()
                .hasTrait(RequiredTrait.class));
    }

    @Test
    public void updatesTaggedUnionShapeWhenMemberChanges() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberAId = ShapeId.from("ns.foo#Container$a");
        ShapeId memberBId = ShapeId.from("ns.foo#Container$b");
        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape memberA = MemberShape.builder().id(memberAId).target(stringId).build();
        MemberShape memberB = MemberShape.builder().id(memberBId).target(stringId).build();
        UnionShape container = UnionShape.builder()
                .id(containerId)
                .addMember(memberA)
                .addMember(memberB)
                .build();
        Model model = Model.builder()
                .addShapes(target, memberA, memberB, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        MemberShape newMemberB = MemberShape.builder()
                .id(memberBId)
                .target(stringId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        Model result = transformer.replaceShapes(model, Arrays.asList(newMemberB));

        // Make sure the member has the trait that was applied.
        assertThat(result.expectShape(memberBId).getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        // Ensure that the union that contains the shape was updated.
        assertThat(result.expectShape(containerId, UnionShape.class).getMember("b").get(),
                   Matchers.is(newMemberB));
    }

    @Test
    public void doesNotOverwriteOtherContainerUpdatesWhenAlsoUpdatingMembers() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberId = ShapeId.from("ns.foo#Container$member");
        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape member = MemberShape.builder().id(memberId).target(stringId).build();
        ListShape container = ListShape.builder().id(containerId).member(member).build();
        Model model = Model.builder()
                .addShapes(target, member, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        ListShape newContainer = container.toBuilder()
                .addTrait(LengthTrait.builder()
                                  .min(1L)
                                  .max(2L)
                                  .sourceLocation(SourceLocation.NONE)
                                  .build())
                .build();
        MemberShape newMember = MemberShape.builder()
                .id(memberId)
                .target(stringId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        Model result = transformer.replaceShapes(model, Arrays.asList(newMember, newContainer));

        // Make sure the member has the trait that was applied.
        assertThat(result.expectShape(memberId).getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        // Ensure that the list shape changes were not overwritten.
        assertThat(result.expectShape(containerId, ListShape.class).getTrait(LengthTrait.class),
                          Matchers.not(Optional.empty()));
        // Ensure that the list shape has the new member.
        assertThat(result.expectShape(containerId, ListShape.class).getMember(), Matchers.is(newMember));
    }
}

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
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
import software.amazon.smithy.model.traits.ReadonlyTrait;

public class RemoveShapesTest {

    private static final ShapeId STRING_TARGET = ShapeId.from("ns.foo#String");

    private void assertContainerMembersAreRemoved(Shape container, List<Shape> members) {
        Model.Builder builder = Model.builder()
                .addShape(container)
                .addShape(StringShape.builder().id(STRING_TARGET).build());
        members.forEach(builder::addShape);
        Model model = builder.build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapes(model, Collections.singleton(container));

        assertThat(result.shapes().count(), Matchers.equalTo(1L));
        assertThat(result.getShape(STRING_TARGET), Matchers.not(Optional.empty()));
        assertThat(result.getShape(container.getId()), Matchers.is(Optional.empty()));
    }

    @Test
    public void removesListMemberWhenRemoved() {
        MemberShape member = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$member"))
                .target(STRING_TARGET)
                .build();
        ListShape container = ListShape.builder().id(ShapeId.from("ns.foo#Container")).member(member).build();
        assertContainerMembersAreRemoved(container, Collections.singletonList(member));
    }

    @Test
    public void removesMapMembersWhenRemoved() {
        MemberShape key = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$key"))
                .target(STRING_TARGET)
                .build();
        MemberShape value = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$value"))
                .target(STRING_TARGET)
                .build();
        MapShape container = MapShape.builder().id(ShapeId.from("ns.foo#Container")).key(key).value(value).build();
        assertContainerMembersAreRemoved(container, Arrays.asList(key, value));
    }

    @Test
    public void removesStructureMembersWhenRemoved() {
        MemberShape a = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$a"))
                .target(STRING_TARGET)
                .build();
        MemberShape b = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$b"))
                .target(STRING_TARGET)
                .build();
        StructureShape container = StructureShape.builder()
                .id(ShapeId.from("ns.foo#Container"))
                .addMember(a)
                .addMember(b)
                .build();
        assertContainerMembersAreRemoved(container, Arrays.asList(a, b));
    }

    @Test
    public void removesTaggedUnionMembersWhenRemoved() {
        MemberShape a = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$a"))
                .target(STRING_TARGET)
                .build();
        MemberShape b = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$b"))
                .target(STRING_TARGET)
                .build();
        UnionShape container = UnionShape.builder()
                .id(ShapeId.from("ns.foo#Container"))
                .addMember(a)
                .addMember(b)
                .build();
        assertContainerMembersAreRemoved(container, Arrays.asList(a, b));
    }

    @Test
    public void removesOperationsFromResourcesWhenOperationRemoved() {
        ResourceShape container = ResourceShape.builder()
                .id(ShapeId.from("ns.foo#Container"))
                .addOperation("ns.foo#A")
                .create(ShapeId.from("ns.foo#B"))
                .addOperation("ns.foo#C")
                .build();
        OperationShape a = OperationShape.builder()
                .id("ns.foo#A")
                .addTrait(new ReadonlyTrait(SourceLocation.NONE))
                .build();
        OperationShape b = OperationShape.builder().id("ns.foo#B").build();
        OperationShape c = OperationShape.builder().id("ns.foo#C").build();

        Model model = Model.builder().addShapes(container, a, b, c).build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapes(model, Arrays.asList(a, b));

        assertThat(result.shapes().count(), Matchers.equalTo(2L));
        assertThat(result.getShape(container.getId()), Matchers.not(Optional.empty()));
        assertThat(result.getShape(c.getId()), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(container.getId()).expectResourceShape().getOperations(),
                   Matchers.contains(c.getId()));
    }
}

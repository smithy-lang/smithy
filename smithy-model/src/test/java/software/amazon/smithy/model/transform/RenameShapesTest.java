/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.Trait;

public class RenameShapesTest {

    @Test
    public void returnsUnmodifiedModelIfGivenEmptyRenameMapping() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        StringShape fooTarget = StringShape.builder().id(stringId).build();
        Model model = Model.builder()
                .addShapes(fooTarget)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        Model result = transformer.renameShapes(model, renamed);

        assertThat(result.shapes().count(), Matchers.equalTo(1L));
        assertThat(result.getShape(stringId).get(), Matchers.is(fooTarget));
    }

    @Test
    public void returnsUnmodifiedModelIfToAndFromAreEqual() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        StringShape target = StringShape.builder().id(stringId).build();
        Model model = Model.builder()
                .addShapes(target)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        renamed.put(stringId, stringId );
        Model result = transformer.renameShapes(model, renamed);

        assertThat(result.shapes().count(), Matchers.equalTo(1L));
        assertThat(result.getShape(stringId).get(), Matchers.is(target));
    }

    @Test
    public void returnsModelWithRenamedStringShape() {
        ShapeId fromStringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId keyId = ShapeId.from("ns.foo#Container$key");
        ShapeId valueId = ShapeId.from("ns.foo#Container$value");

        StringShape target = StringShape.builder().id(fromStringId).build();
        MemberShape keyMember = MemberShape.builder().id(keyId).target(fromStringId).build();
        MemberShape valueMember = MemberShape.builder().id(valueId).target(fromStringId).build();
        MapShape container = MapShape.builder().id(containerId).key(keyMember).value(valueMember).build();
        Model model = Model.builder()
                .addShapes(target, keyMember, valueMember, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();

        ShapeId toStringId = ShapeId.from("ns.bar#String");
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        renamed.put(fromStringId, toStringId );
        Model result = transformer.renameShapes(model, renamed);

        assertThat(result.getShape(toStringId).isPresent(), Matchers.is(true));
        assertThat(result.getShape(fromStringId).isPresent(), Matchers.is(false));
    }

    @Test
    public void updatesIdRefsWhenValueRenamed() {
        Model model = Model.assembler()
                .addImport(IntegTest.class.getResource("rename-shape-test-model.json"))
                .assemble()
                .unwrap();

        ShapeId fromId = ShapeId.from("ns.foo#OldShape");
        ShapeId toId = ShapeId.from("ns.foo#NewShape");
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        renamed.put(fromId, toId);
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.renameShapes(model, renamed);
        StringNode node = Node.from(toId.toShapeId().toString());

        assertThat(result.getShape(toId).isPresent(), Matchers.is(true));
        Shape shape = result.expectShape(ShapeId.from("ns.foo#ValidShape"));
        Trait trait = shape.findTrait("ns.foo#integerRef").get();
        assertThat(trait.toNode(), Matchers.equalTo(node));
        assertThat(result.getShape(fromId).isPresent(), Matchers.is(false));
    }

    @Test
    public void testArrayNode() {
        Model model = Model.assembler()
                .addImport(IntegTest.class.getResource("test-model.json"))
                .assemble()
                .unwrap();

        ShapeId fromId = ShapeId.from("ns.foo#MyOperation");
        ShapeId toId = ShapeId.from("ns.foo#MyNewOperation");
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        renamed.put(fromId, toId);
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.renameShapes(model, renamed);

        assertThat(result.getShape(toId).isPresent(), Matchers.is(true));
        assertThat(result.getShape(fromId).isPresent(), Matchers.is(false));
    }

    @Test
    public void updatesListMembersWhenContainerUpdated() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberId = ShapeId.from("ns.foo#Container$member");

        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape member = MemberShape.builder().id(memberId).target(target).build();
        ListShape container = ListShape.builder().id(containerId).addMember(member).build();
        Model model = Model.builder()
                .addShapes(target, member, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        ShapeId newContainerId = ShapeId.from("ns.bar#Baz");
        ShapeId newMemberId = ShapeId.from("ns.bar#Baz$member");
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        renamed.put(containerId, newContainerId);
        Model result = transformer.renameShapes(model, renamed);

        assertThat(result.getShape(newContainerId).isPresent(), Matchers.is(true));
        ListShape newContainer = result.getShape(newContainerId).get().asListShape().get();
        assertThat(newContainer.getMember().getId(), Matchers.is(newMemberId));
        assertThat(result.getShape(containerId).isPresent(), Matchers.is(false));
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
        ShapeId newContainerId = ShapeId.from("ns.bar#Baz");
        ShapeId newKeyId = ShapeId.from("ns.bar#Baz$key");
        ShapeId newValueId = ShapeId.from("ns.bar#Baz$value");
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        renamed.put(containerId, newContainerId);
        Model result = transformer.renameShapes(model, renamed);

        assertThat(result.getShape(newContainerId).isPresent(), Matchers.is(true));
        MapShape newContainer = result.getShape(newContainerId).get().asMapShape().get();
        assertThat(newContainer.getKey().getId(), Matchers.is(newKeyId));
        assertThat(newContainer.getValue().getId(), Matchers.is(newValueId));
        assertThat(result.getShape(containerId).isPresent(), Matchers.is(false));
    }

    @Test
    public void updatesSetMembersWhenContainerUpdated() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberId = ShapeId.from("ns.foo#Container$member");

        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape member = MemberShape.builder().id(memberId).target(target).build();
        SetShape container = SetShape.builder().id(containerId).addMember(member).build();
        Model model = Model.builder()
                .addShapes(target, member, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        ShapeId newContainerId = ShapeId.from("ns.bar#Baz");
        ShapeId newMemberId = ShapeId.from("ns.bar#Baz$member");
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        renamed.put(containerId, newContainerId);
        Model result = transformer.renameShapes(model, renamed);

        assertThat(result.getShape(newContainerId).isPresent(), Matchers.is(true));
        SetShape newContainer = result.getShape(newContainerId).get().asSetShape().get();
        assertThat(newContainer.getMember().getId(), Matchers.is(newMemberId));
        assertThat(result.getShape(containerId).isPresent(), Matchers.is(false));
    }

    @Test
    public void updatesStructureMembersWhenContainerUpdated() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberId = ShapeId.from("ns.foo#Container$member");

        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape member = MemberShape.builder().id(memberId).target(target).build();
        StructureShape container = StructureShape.builder().id(containerId).addMember(member).build();
        Model model = Model.builder()
                .addShapes(target, member, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        ShapeId newContainerId = ShapeId.from("ns.bar#Baz");
        ShapeId newMemberId = ShapeId.from("ns.bar#Baz$member");
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        renamed.put(containerId, newContainerId);
        Model result = transformer.renameShapes(model, renamed);

        assertThat(result.getShape(newContainerId).isPresent(), Matchers.is(true));
        StructureShape newContainer = result.getShape(newContainerId).get().asStructureShape().get();
        newContainer.getMember("member").ifPresent(newMember -> {
            assertThat(newMember.getId(), Matchers.is(newMemberId));
        });
        assertThat(result.getShape(containerId).isPresent(), Matchers.is(false));
    }

    @Test
    public void updatesUnionMembersWhenContainerUpdated() {
        ShapeId stringId = ShapeId.from("ns.foo#String");
        ShapeId containerId = ShapeId.from("ns.foo#Container");
        ShapeId memberId = ShapeId.from("ns.foo#Container$member");

        StringShape target = StringShape.builder().id(stringId).build();
        MemberShape member = MemberShape.builder().id(memberId).target(target).build();
        UnionShape container = UnionShape.builder().id(containerId).addMember(member).build();
        Model model = Model.builder()
                .addShapes(target, member, container)
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        ShapeId newContainerId = ShapeId.from("ns.bar#Baz");
        ShapeId newMemberId = ShapeId.from("ns.bar#Baz$member");
        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        renamed.put(containerId, newContainerId);
        Model result = transformer.renameShapes(model, renamed);

        assertThat(result.getShape(newContainerId).isPresent(), Matchers.is(true));
        UnionShape newContainer = result.getShape(newContainerId).get().asUnionShape().get();
        newContainer.getMember("member").ifPresent(newMember -> {
            assertThat(newMember.getId(), Matchers.is(newMemberId));
        });
        assertThat(result.getShape(containerId).isPresent(), Matchers.is(false));
    }
}

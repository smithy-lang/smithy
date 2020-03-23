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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
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

        assertEquals(result.shapes().count(), 1);
        assertEquals(result.getShape(stringId).get(), fooTarget);
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

        assertEquals(result.shapes().count(), 1);
        assertEquals(result.getShape(stringId).get(), target);
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

        assertTrue(result.getShape(toStringId).isPresent());
        assertFalse(result.getShape(fromStringId).isPresent());
    }

    @Test
    public void updatesMetadataReferences() {
        Model model = Model.assembler()
                .addImport(IntegTest.class.getResource("rename-shape-test-model.json"))
                .assemble()
                .unwrap();

        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        ShapeId fooUnreferenced = ShapeId.from("ns.foo#UnreferencedString");
        ShapeId barUnreferenced = ShapeId.from("ns.bar#UnreferencedString");
        renamed.put(fooUnreferenced, barUnreferenced);
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.renameShapes(model, renamed);
        ArrayNode suppressions = result.getMetadata().get("suppressions").asArrayNode().get();
        ObjectNode suppression = suppressions.getElements().get(0).asObjectNode().get();
        ArrayNode suppressionShapes = suppression.getStringMap().get("shapes").expectArrayNode();
        StringNode suppressionShape = suppressionShapes.getElements().get(0).asStringNode().get();

        assertEquals(suppressionShape.getValue(), "ns.bar#UnreferencedString");
        assertTrue(result.getShape(barUnreferenced).isPresent());
        assertFalse(result.getShape(fooUnreferenced).isPresent());
    }

    @Test
    public void updatesIdRefValues() {
        Model model = Model.assembler()
                .addImport(IntegTest.class.getResource("rename-shape-test-model.json"))
                .assemble()
                .unwrap();

        Map<ShapeId, ShapeId> renamed = new HashMap<>();
        ShapeId fromId = ShapeId.from("ns.foo#OldShape");
        ShapeId toId = ShapeId.from("ns.foo#NewShape");
        renamed.put(fromId, toId);
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.renameShapes(model, renamed);
        StringNode node = Node.from(toId.toShapeId().toString());

        assertTrue(result.getShape(toId).isPresent());
        assertFalse(result.getShape(fromId).isPresent());
        Shape shape = result.expectShape(ShapeId.from("ns.foo#ValidShape"));
        Trait trait = shape.findTrait("ns.foo#integerRef").get();
        assertEquals(trait.toNode(), node);
    }

    @Test
    public void updatesShapeNamesAndReferences() {
        Model model = Model.assembler()
                .addImport(IntegTest.class.getResource("rename-shape-test-model.json"))
                .assemble()
                .unwrap();
        Map<ShapeId, ShapeId> renamed = new HashMap<>();

        // Service
        ShapeId fromService = ShapeId.from("ns.foo#MyService");
        ShapeId toService = ShapeId.from("ns.bar#MyNewService");
        renamed.put(fromService,toService);

        // Operation
        ShapeId fromOperation = ShapeId.from("ns.foo#MyOperation");
        ShapeId toOperation = ShapeId.from("ns.baz#MyNewOperation");
        renamed.put(fromOperation,toOperation);

        // Resource
        ShapeId fromResource = ShapeId.from("ns.foo#MyResource");
        ShapeId toResource = ShapeId.from("ns.qux#MyNewResource");
        renamed.put(fromResource, toResource);

        // Structure
        ShapeId fromStructure = ShapeId.from("ns.foo#MyStructure");
        ShapeId toStructure = ShapeId.from("ns.quux#MyNewStructure");
        renamed.put(fromStructure, toStructure);

        // List
        ShapeId fromList = ShapeId.from("ns.foo#MyList");
        ShapeId toList = ShapeId.from("ns.quuz#MyNewList");
        renamed.put(fromList, toList);

        // Map
        ShapeId fromMap = ShapeId.from("ns.foo#MyMap");
        ShapeId toMap = ShapeId.from("ns.corge#MyNewMap");
        renamed.put(fromMap, toMap);

        // Set
        ShapeId fromSet = ShapeId.from("ns.foo#MySet");
        ShapeId toSet = ShapeId.from("ns.grault#MyNewSet");
        renamed.put(fromSet, toSet);

        // Union
        ShapeId fromUnion = ShapeId.from("ns.foo#MyUnion");
        ShapeId toUnion = ShapeId.from("ns.garply#MyNewUnion");
        renamed.put(fromUnion, toUnion);

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.renameShapes(model, renamed);

        // All new names are present.
        assertTrue(result.getShape(toService).isPresent());
        assertTrue(result.getShape(toOperation).isPresent());
        assertTrue(result.getShape(toResource).isPresent());
        assertTrue(result.getShape(toStructure).isPresent());
        assertTrue(result.getShape(toList).isPresent());
        assertTrue(result.getShape(toMap).isPresent());
        assertTrue(result.getShape(toSet).isPresent());
        assertTrue(result.getShape(toUnion).isPresent());

        // All new shapes are updated as references.
        StructureShape operationInput = result.getShape(ShapeId.from("ns.foo#MyOperationInput")).get()
                .asStructureShape().get();
        MemberShape struct = operationInput.getMember("struct").get();
        MemberShape list = operationInput.getMember("list").get();
        MemberShape map = operationInput.getMember("map").get();
        MemberShape set = operationInput.getMember("set").get();
        MemberShape union = operationInput.getMember("union").get();
        assertEquals(struct.getTarget(), toStructure);
        assertEquals(list.getTarget(), toList);
        assertEquals(map.getTarget(), toMap);
        assertEquals(set.getTarget(), toSet);
        assertEquals(union.getTarget(), toUnion);

        // All old names have been removed.
        assertFalse(result.getShape(fromService).isPresent());
        assertFalse(result.getShape(fromOperation).isPresent());
        assertFalse(result.getShape(fromResource).isPresent());
        assertFalse(result.getShape(fromStructure).isPresent());
        assertFalse(result.getShape(fromList).isPresent());
        assertFalse(result.getShape(fromMap).isPresent());
        assertFalse(result.getShape(fromSet).isPresent());
        assertFalse(result.getShape(fromUnion).isPresent());
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

        assertTrue(result.getShape(newContainerId).isPresent());
        ListShape newContainer = result.getShape(newContainerId).get().asListShape().get();
        assertEquals(newContainer.getMember().getId(), newMemberId);
        assertFalse(result.getShape(containerId).isPresent());
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

        assertTrue(result.getShape(newContainerId).isPresent());
        MapShape newContainer = result.getShape(newContainerId).get().asMapShape().get();
        assertEquals(newContainer.getKey().getId(), newKeyId);
        assertEquals(newContainer.getValue().getId(), newValueId);
        assertFalse(result.getShape(containerId).isPresent());
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

        assertTrue(result.getShape(newContainerId).isPresent());
        SetShape newContainer = result.getShape(newContainerId).get().asSetShape().get();
        assertEquals(newContainer.getMember().getId(), newMemberId);
        assertFalse(result.getShape(containerId).isPresent());
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

        assertTrue(result.getShape(newContainerId).isPresent());
        StructureShape newContainer = result.getShape(newContainerId).get().asStructureShape().get();
        newContainer.getMember("member").ifPresent(newMember -> {
            assertEquals(newMember.getId(), newMemberId);
        });
        assertFalse(result.getShape(containerId).isPresent());
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

        assertTrue(result.getShape(newContainerId).isPresent());
        UnionShape newContainer = result.getShape(newContainerId).get().asUnionShape().get();
        newContainer.getMember("member").ifPresent(newMember -> {
            assertEquals(newMember.getId(), newMemberId);
        });
        assertFalse(result.getShape(containerId).isPresent());
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;

public class BottomUpNeighborVisitorTest {

    @Test
    public void dataShape() {
        Shape shape = BlobShape.builder().id("ns.foo#name").build();
        MemberShape listMemberShape = MemberShape.builder()
                .target(shape.getId())
                .id("ns.foo#list$member")
                .build();
        ListShape listShape = ListShape.builder().id("ns.foo#list").member(listMemberShape).build();

        MemberShape mapKeyShape = MemberShape.builder()
                .target(shape.getId())
                .id("ns.foo#map$key")
                .build();
        MemberShape mapValueShape = MemberShape.builder()
                .target(shape.getId())
                .id("ns.foo#map$value")
                .build();
        MapShape mapShape = MapShape.builder()
                .id(mapKeyShape.getId().withoutMember())
                .key(mapKeyShape)
                .value(mapValueShape)
                .build();

        MemberShape structureMemberShape = MemberShape.builder()
                .target(shape.getId())
                .id("ns.foo#structure$aMember")
                .build();
        StructureShape structureShape = StructureShape.builder()
                .id(structureMemberShape.getId().withoutMember())
                .addMember(structureMemberShape)
                .build();

        Model model = Model.builder()
                .addShapes(shape, listShape, mapShape, structureShape)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.reverse(model);

        assertThat(neighborVisitor.getNeighbors(shape),
                containsInAnyOrder(
                        Relationship.create(listMemberShape, RelationshipType.MEMBER_TARGET, shape),
                        Relationship.create(mapKeyShape, RelationshipType.MEMBER_TARGET, shape),
                        Relationship.create(mapValueShape, RelationshipType.MEMBER_TARGET, shape),
                        Relationship.create(structureMemberShape, RelationshipType.MEMBER_TARGET, shape)));
    }

    @Test
    public void memberShape() {
        Shape shape = BlobShape.builder().id("ns.foo#name").build();
        MemberShape listMemberShape = MemberShape.builder()
                .target(shape.getId())
                .id("ns.foo#list$member")
                .build();
        Shape listShape = ListShape.builder().member(listMemberShape).id("ns.foo#list").build();
        MemberShape mapKeyShape = MemberShape.builder()
                .target(shape.getId())
                .id("ns.foo#map$key")
                .build();
        MemberShape mapValueShape = MemberShape.builder()
                .target(shape.getId())
                .id("ns.foo#map$value")
                .build();
        Shape mapShape = MapShape.builder()
                .key(mapKeyShape)
                .value(mapValueShape)
                .id("ns.foo#map")
                .build();
        MemberShape structureMemberShape = MemberShape.builder()
                .target(shape.getId())
                .id("ns.foo#structure$aMember")
                .build();
        Shape structureShape = StructureShape.builder().addMember(structureMemberShape).id("ns.foo#structure").build();
        MemberShape tuMemberShape = MemberShape.builder()
                .target(shape.getId())
                .id("ns.foo#union$aMember")
                .build();
        Shape unionShape = UnionShape.builder().addMember(tuMemberShape).id("ns.foo#union").build();
        Model model = Model.builder()
                .addShape(shape)
                .addShape(listMemberShape)
                .addShape(listShape)
                .addShape(mapKeyShape)
                .addShape(mapValueShape)
                .addShape(mapShape)
                .addShape(structureMemberShape)
                .addShape(structureShape)
                .addShape(tuMemberShape)
                .addShape(unionShape)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.reverse(model);

        assertThat(neighborVisitor.getNeighbors(listMemberShape),
                containsInAnyOrder(
                        Relationship.create(listShape, RelationshipType.LIST_MEMBER, listMemberShape)));
        assertThat(neighborVisitor.getNeighbors(mapKeyShape),
                containsInAnyOrder(
                        Relationship.create(mapShape, RelationshipType.MAP_KEY, mapKeyShape)));
        assertThat(neighborVisitor.getNeighbors(mapValueShape),
                containsInAnyOrder(
                        Relationship.create(mapShape, RelationshipType.MAP_VALUE, mapValueShape)));
        assertThat(neighborVisitor.getNeighbors(structureMemberShape),
                containsInAnyOrder(
                        Relationship.create(structureShape, RelationshipType.STRUCTURE_MEMBER, structureMemberShape)));
        assertThat(neighborVisitor.getNeighbors(tuMemberShape),
                containsInAnyOrder(
                        Relationship.create(unionShape, RelationshipType.UNION_MEMBER, tuMemberShape)));
    }

    @Test
    public void structureShape() {
        StructureShape error = StructureShape.builder().id("ns.foo#Error").build();
        StructureShape input = StructureShape.builder().id("ns.foo#Input").build();
        StructureShape output = StructureShape.builder().id("ns.foo#Output").build();
        OperationShape fooOperation = OperationShape.builder()
                .id("ns.foo#Foo")
                .input(input.getId())
                .output(output.getId())
                .addError(error.getId())
                .build();
        OperationShape barOperation = OperationShape.builder()
                .id("ns.foo#Bar")
                .input(output.getId())
                .output(input.getId())
                .addError(error.getId())
                .build();
        Model model = Model.builder().addShapes(fooOperation, barOperation, input, output, error).build();
        NeighborProvider neighborVisitor = NeighborProvider.reverse(model);

        assertThat(neighborVisitor.getNeighbors(input),
                containsInAnyOrder(
                        Relationship.create(fooOperation, RelationshipType.INPUT, input),
                        Relationship.create(barOperation, RelationshipType.OUTPUT, input)));
        assertThat(neighborVisitor.getNeighbors(output),
                containsInAnyOrder(
                        Relationship.create(fooOperation, RelationshipType.OUTPUT, output),
                        Relationship.create(barOperation, RelationshipType.INPUT, output)));
        assertThat(neighborVisitor.getNeighbors(error),
                containsInAnyOrder(
                        Relationship.create(fooOperation, RelationshipType.ERROR, error),
                        Relationship.create(barOperation, RelationshipType.ERROR, error)));
    }

    @Test
    public void serviceShape() {
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Svc")
                .version("2017-01-17")
                .addOperation("ns.foo#Operation")
                .addResource("ns.foo#Resource")
                .build();
        OperationShape operationShape = OperationShape.builder().id("ns.foo#Operation").build();
        ResourceShape resourceShape = ResourceShape.builder().id("ns.foo#Resource").build();
        Model model = Model.builder()
                .addShapes(service, resourceShape, operationShape)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.reverse(model);

        assertThat(neighborVisitor.getNeighbors(service), empty());
    }

    @Test
    public void resourceShape() {
        ServiceShape parent = ServiceShape.builder()
                .id("ns.foo#Svc")
                .version("2017-01-17")
                .addResource("ns.foo#Resource")
                .build();
        ServiceShape otherService = ServiceShape.builder()
                .id("ns.foo#OtherSvc")
                .version("2017-01-17")
                .addResource("ns.foo#Child1")
                .build();
        ResourceShape resource = ResourceShape.builder()
                .id("ns.foo#Resource")
                .addIdentifier("id", "smithy.api#String")
                .addResource("ns.foo#Child1")
                .build();
        ResourceShape child1 = ResourceShape.builder().id("ns.foo#Child1").addResource("ns.foo#Child2").build();
        ResourceShape child2 = ResourceShape.builder().id("ns.foo#Child2").build();
        Model model = Model.builder()
                .addShapes(parent, resource, child1, child2)
                .addShape(otherService)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.reverse(model);

        assertThat(neighborVisitor.getNeighbors(child2),
                containsInAnyOrder(
                        Relationship.create(child1, RelationshipType.RESOURCE, child2)));
        assertThat(neighborVisitor.getNeighbors(child1),
                containsInAnyOrder(
                        Relationship.create(resource, RelationshipType.RESOURCE, child1),
                        Relationship.create(otherService, RelationshipType.RESOURCE, child1)));
        assertThat(neighborVisitor.getNeighbors(resource),
                containsInAnyOrder(
                        Relationship.create(parent, RelationshipType.RESOURCE, resource)));
        assertThat(neighborVisitor.getNeighbors(child2),
                containsInAnyOrder(
                        Relationship.create(child1, RelationshipType.RESOURCE, child2)));
    }

    @Test
    public void operationShape() {
        ServiceShape parent = ServiceShape.builder()
                .id("ns.foo#Svc")
                .version("2017-01-17")
                .addResource("ns.foo#Resource")
                .build();
        ServiceShape otherService = ServiceShape.builder()
                .id("ns.foo#OtherSvc")
                .version("2017-01-17")
                .addOperation("ns.foo#Named")
                .build();
        ResourceShape resource = ResourceShape.builder()
                .id("ns.foo#Resource")
                .addIdentifier("id", "smithy.api#String")
                .put(ShapeId.from("ns.foo#Put"))
                .create(ShapeId.from("ns.foo#Create"))
                .read(ShapeId.from("ns.foo#Get"))
                .update(ShapeId.from("ns.foo#Update"))
                .delete(ShapeId.from("ns.foo#Delete"))
                .list(ShapeId.from("ns.foo#List"))
                .addOperation("ns.foo#Named")
                .addCollectionOperation("ns.foo#GenericCollection")
                .build();
        OperationShape createOperation = OperationShape.builder().id("ns.foo#Create").build();
        OperationShape getOperation = OperationShape.builder()
                .id("ns.foo#Get")
                .addTrait(new ReadonlyTrait())
                .build();
        OperationShape updateOperation = OperationShape.builder().id("ns.foo#Update").build();
        OperationShape deleteOperation = OperationShape.builder()
                .id("ns.foo#Delete")
                .addTrait(new IdempotentTrait())
                .build();
        OperationShape listOperation = OperationShape.builder()
                .id("ns.foo#List")
                .addTrait(new ReadonlyTrait())
                .build();
        OperationShape namedOperation = OperationShape.builder()
                .id("ns.foo#Named")
                .build();
        OperationShape collectionOperation = OperationShape.builder()
                .id("ns.foo#GenericCollection")
                .build();
        OperationShape putOperation = OperationShape.builder()
                .id("ns.foo#Put")
                .build();
        Model model = Model.builder()
                .addShape(parent)
                .addShapes(resource, createOperation, getOperation, updateOperation, deleteOperation, listOperation)
                .addShapes(otherService, namedOperation, collectionOperation, putOperation)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.reverse(model);

        assertThat(neighborVisitor.getNeighbors(namedOperation),
                containsInAnyOrder(
                        Relationship.create(resource, RelationshipType.OPERATION, namedOperation),
                        Relationship.create(otherService, RelationshipType.OPERATION, namedOperation)));
        assertThat(neighborVisitor.getNeighbors(createOperation),
                containsInAnyOrder(
                        Relationship.create(resource, RelationshipType.CREATE, createOperation)));
        assertThat(neighborVisitor.getNeighbors(getOperation),
                containsInAnyOrder(
                        Relationship.create(resource, RelationshipType.READ, getOperation)));
        assertThat(neighborVisitor.getNeighbors(updateOperation),
                containsInAnyOrder(
                        Relationship.create(resource, RelationshipType.UPDATE, updateOperation)));
        assertThat(neighborVisitor.getNeighbors(deleteOperation),
                containsInAnyOrder(
                        Relationship.create(resource, RelationshipType.DELETE, deleteOperation)));
        assertThat(neighborVisitor.getNeighbors(listOperation),
                containsInAnyOrder(
                        Relationship.create(resource, RelationshipType.LIST, listOperation)));
        assertThat(neighborVisitor.getNeighbors(putOperation),
                containsInAnyOrder(
                        Relationship.create(resource, RelationshipType.PUT, putOperation)));
        assertThat(neighborVisitor.getNeighbors(collectionOperation),
                containsInAnyOrder(
                        Relationship.create(resource, RelationshipType.COLLECTION_OPERATION, collectionOperation)));
    }

    @Test
    public void returnsEmptyOnUnreferencedShape() {
        StringShape target = StringShape.builder()
                .id("ns.foo#String")
                .build();
        Model model = Model.builder()
                .addShape(target)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.reverse(model);

        assertThat(neighborVisitor.getNeighbors(target), empty());
    }
}

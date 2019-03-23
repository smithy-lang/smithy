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

package software.amazon.smithy.model.neighbor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;

public class BottomUpNeighborVisitorTest {

    @Test
    public void dataShape() {
        Shape shape = BlobShape.builder().id("ns.foo#name").build();
        Shape listMemberShape = MemberShape.builder().target(shape.getId())
                .id("ns.foo#list$member")
                .build();
        Shape mapKeyShape = MemberShape.builder().target(shape.getId())
                .id("ns.foo#map$key")
                .build();
        Shape mapValueShape = MemberShape.builder().target(shape.getId())
                .id("ns.foo#map$value")
                .build();
        Shape structureMemberShape = MemberShape.builder().target(shape.getId())
                .id("ns.foo#structure$aMember")
                .build();
        ShapeIndex shapeIndex = ShapeIndex.builder()
                .addShape(shape)
                .addShape(listMemberShape)
                .addShape(mapKeyShape)
                .addShape(mapValueShape)
                .addShape(structureMemberShape)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.bottomUp(shapeIndex);

        assertThat(neighborVisitor.getNeighbors(shape), containsInAnyOrder(
                new Relationship(listMemberShape, RelationshipType.MEMBER_TARGET, shape.getId(), shape),
                new Relationship(mapKeyShape, RelationshipType.MEMBER_TARGET, shape.getId(), shape),
                new Relationship(mapValueShape, RelationshipType.MEMBER_TARGET, shape.getId(), shape),
                new Relationship(structureMemberShape, RelationshipType.MEMBER_TARGET, shape.getId(), shape)));
    }

    @Test
    public void memberShape() {
        Shape shape = BlobShape.builder().id("ns.foo#name").build();
        MemberShape listMemberShape = MemberShape.builder().target(shape.getId())
                .id("ns.foo#list$member")
                .build();
        Shape listShape = ListShape.builder().member(listMemberShape).id("ns.foo#list").build();
        MemberShape mapKeyShape = MemberShape.builder().target(shape.getId())
                .id("ns.foo#map$key")
                .build();
        MemberShape mapValueShape = MemberShape.builder().target(shape.getId())
                .id("ns.foo#map$value")
                .build();
        Shape mapShape = MapShape.builder()
                .key(mapKeyShape)
                .value(mapValueShape)
                .id("ns.foo#map")
                .build();
        MemberShape structureMemberShape = MemberShape.builder().target(shape.getId())
                .id("ns.foo#structure$aMember")
                .build();
        Shape structureShape = StructureShape.builder().addMember(structureMemberShape).id("ns.foo#structure").build();
        MemberShape tuMemberShape = MemberShape.builder().target(shape.getId())
                .id("ns.foo#union$aMember")
                .build();
        Shape unionShape = UnionShape.builder().addMember(tuMemberShape).id("ns.foo#union").build();
        ShapeIndex shapeIndex = ShapeIndex.builder()
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
        NeighborProvider neighborVisitor = NeighborProvider.bottomUp(shapeIndex);

        assertThat(neighborVisitor.getNeighbors(listMemberShape), containsInAnyOrder(
                new Relationship(listShape, RelationshipType.LIST_MEMBER, listMemberShape.getId(), listMemberShape)));
        assertThat(neighborVisitor.getNeighbors(mapKeyShape), containsInAnyOrder(
                new Relationship(mapShape, RelationshipType.MAP_KEY, mapKeyShape.getId(), mapKeyShape)));
        assertThat(neighborVisitor.getNeighbors(mapValueShape), containsInAnyOrder(
                new Relationship(mapShape, RelationshipType.MAP_VALUE, mapValueShape.getId(), mapValueShape)));
        assertThat(neighborVisitor.getNeighbors(structureMemberShape), containsInAnyOrder(
                new Relationship(structureShape, RelationshipType.STRUCTURE_MEMBER, structureMemberShape.getId(),
                        structureMemberShape)));
        assertThat(neighborVisitor.getNeighbors(tuMemberShape), containsInAnyOrder(
                new Relationship(unionShape, RelationshipType.UNION_MEMBER, tuMemberShape.getId(),
                        tuMemberShape)));
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
        ShapeIndex shapeIndex = ShapeIndex.builder().addShapes(fooOperation, barOperation, input, output, error).build();
        NeighborProvider neighborVisitor = NeighborProvider.bottomUp(shapeIndex);

        assertThat(neighborVisitor.getNeighbors(input), containsInAnyOrder(
                new Relationship(fooOperation, RelationshipType.INPUT, input.getId(), input),
                new Relationship(barOperation, RelationshipType.OUTPUT, input.getId(), input)));
        assertThat(neighborVisitor.getNeighbors(output), containsInAnyOrder(
                new Relationship(fooOperation, RelationshipType.OUTPUT, output.getId(), output),
                new Relationship(barOperation, RelationshipType.INPUT, output.getId(), output)));
        assertThat(neighborVisitor.getNeighbors(error), containsInAnyOrder(
                new Relationship(fooOperation, RelationshipType.ERROR, error.getId(), error),
                new Relationship(barOperation, RelationshipType.ERROR, error.getId(), error)));
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
        ShapeIndex shapeIndex = ShapeIndex.builder()
                .addShapes(service, resourceShape, operationShape)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.bottomUp(shapeIndex);

        assertThat(neighborVisitor.getNeighbors(service), containsInAnyOrder(
                new Relationship(resourceShape, RelationshipType.BOUND, service.getId(), service),
                new Relationship(operationShape, RelationshipType.BOUND, service.getId(), service)
        ));
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
        ShapeIndex shapeIndex = ShapeIndex.builder()
                .addShapes(parent, resource, child1, child2)
                .addShape(otherService)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.bottomUp(shapeIndex);

        assertThat(neighborVisitor.getNeighbors(child2), containsInAnyOrder(
                new Relationship(child1, RelationshipType.RESOURCE, child2.getId(), child2)));
        assertThat(neighborVisitor.getNeighbors(child1), containsInAnyOrder(
                new Relationship(resource, RelationshipType.RESOURCE, child1.getId(), child1),
                new Relationship(otherService, RelationshipType.RESOURCE, child1.getId(), child1),
                new Relationship(child2, RelationshipType.BOUND, child1.getId(), child1)));
        assertThat(neighborVisitor.getNeighbors(resource), containsInAnyOrder(
                new Relationship(parent, RelationshipType.RESOURCE, resource.getId(), resource),
                new Relationship(child1, RelationshipType.BOUND, resource.getId(), resource)));
        assertThat(neighborVisitor.getNeighbors(child2), containsInAnyOrder(
                new Relationship(child1, RelationshipType.RESOURCE, child2.getId(), child2)));
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
                .create(ShapeId.from("ns.foo#Create"))
                .read(ShapeId.from("ns.foo#Get"))
                .update(ShapeId.from("ns.foo#Update"))
                .delete(ShapeId.from("ns.foo#Delete"))
                .list(ShapeId.from("ns.foo#List"))
                .addOperation("ns.foo#Named")
                .build();
        OperationShape createOperation = OperationShape.builder().id("ns.foo#Create").build();
        OperationShape getOperation = OperationShape.builder()
                .id("ns.foo#Get")
                .addTrait(new ReadonlyTrait(SourceLocation.NONE))
                .build();
        OperationShape updateOperation = OperationShape.builder().id("ns.foo#Update").build();
        OperationShape deleteOperation = OperationShape.builder()
                .id("ns.foo#Delete")
                .addTrait(new IdempotentTrait(SourceLocation.NONE))
                .build();
        OperationShape listOperation = OperationShape.builder()
                .id("ns.foo#List")
                .addTrait(new ReadonlyTrait(SourceLocation.NONE))
                .build();
        OperationShape namedOperation = OperationShape.builder()
                .id("ns.foo#Named")
                .build();
        ShapeIndex shapeIndex = ShapeIndex.builder()
                .addShape(parent)
                .addShapes(resource, createOperation, getOperation, updateOperation, deleteOperation, listOperation)
                .addShapes(otherService, namedOperation)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.bottomUp(shapeIndex);

        assertThat(neighborVisitor.getNeighbors(namedOperation), containsInAnyOrder(
                new Relationship(resource, RelationshipType.OPERATION, namedOperation.getId(), namedOperation),
                new Relationship(otherService, RelationshipType.OPERATION, namedOperation.getId(), namedOperation)));
        assertThat(neighborVisitor.getNeighbors(createOperation), containsInAnyOrder(
                new Relationship(resource, RelationshipType.CREATE, createOperation.getId(), createOperation),
                new Relationship(resource, RelationshipType.OPERATION, createOperation.getId(), createOperation)));
        assertThat(neighborVisitor.getNeighbors(getOperation), containsInAnyOrder(
                new Relationship(resource, RelationshipType.READ, getOperation.getId(), getOperation),
                new Relationship(resource, RelationshipType.OPERATION, getOperation.getId(), getOperation)));
        assertThat(neighborVisitor.getNeighbors(updateOperation), containsInAnyOrder(
                new Relationship(resource, RelationshipType.UPDATE, updateOperation.getId(), updateOperation),
                new Relationship(resource, RelationshipType.OPERATION, updateOperation.getId(), updateOperation)));
        assertThat(neighborVisitor.getNeighbors(deleteOperation), containsInAnyOrder(
                new Relationship(resource, RelationshipType.DELETE, deleteOperation.getId(), deleteOperation),
                new Relationship(resource, RelationshipType.OPERATION, deleteOperation.getId(), deleteOperation)));
        assertThat(neighborVisitor.getNeighbors(listOperation), containsInAnyOrder(
                new Relationship(resource, RelationshipType.LIST, listOperation.getId(), listOperation),
                new Relationship(resource, RelationshipType.OPERATION, listOperation.getId(), listOperation)));
    }

    @Test
    public void returnsEmptyOnUnreferencedShape() {
        StringShape target = StringShape.builder()
                .id("ns.foo#String")
                .build();
        ShapeIndex shapeIndex = ShapeIndex.builder()
                .addShape(target)
                .build();
        NeighborProvider neighborVisitor = NeighborProvider.bottomUp(shapeIndex);

        assertThat(neighborVisitor.getNeighbors(target), empty());
    }
}

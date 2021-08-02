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

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.utils.ListUtils;

/**
 * Finds all neighbors of a shape, returning them as a list of
 * {@link Relationship} objects.
 *
 * <p>Each neighbor shape that is not in the provided model will
 * result in a relationship where the optional
 * {@link Relationship#getNeighborShape() neighbor shape} is empty.
 *
 * @see NeighborProvider#of
 */
final class NeighborVisitor extends ShapeVisitor.Default<List<Relationship>> implements NeighborProvider {
    private final Model model;

    NeighborVisitor(Model model) {
        this.model = model;
    }

    @Override
    public List<Relationship> getNeighbors(Shape shape) {
        return shape.accept(this);
    }

    @Override
    public List<Relationship> getDefault(Shape shape) {
        return ListUtils.of();
    }

    @Override
    public List<Relationship> serviceShape(ServiceShape shape) {
        List<Relationship> result = new ArrayList<>();
        // Add OPERATION from service -> operation. Add BINDING from operation -> service.
        for (ShapeId operation : shape.getOperations()) {
            addBinding(result, shape, operation, RelationshipType.OPERATION);
        }
        // Add RESOURCE from service -> resource. Add BINDING from resource -> service.
        for (ShapeId resource : shape.getResources()) {
            addBinding(result, shape, resource, RelationshipType.RESOURCE);
        }
        // Add ERROR relationships from service -> errors.
        for (ShapeId errorId : shape.getErrors()) {
            result.add(relationship(shape, RelationshipType.ERROR, errorId));
        }
        return result;
    }

    private void addBinding(List<Relationship> result, Shape container, ShapeId bindingTarget, RelationshipType type) {
        result.add(relationship(container, type, bindingTarget));
        addBound(result, container, bindingTarget);
    }

    private void addBound(List<Relationship> result, Shape container, ShapeId bindingTarget) {
        model.getShape(bindingTarget)
                .ifPresent(op -> result.add(relationship(op, RelationshipType.BOUND, container.getId())));
    }

    @Override
    public List<Relationship> resourceShape(ResourceShape shape) {
        List<Relationship> result = new ArrayList<>();
        // Add IDENTIFIER relationships.
        shape.getIdentifiers().forEach((k, v) -> result.add(relationship(shape, RelationshipType.IDENTIFIER, v)));
        // Add RESOURCE from resourceA -> resourceB and BOUND from resourceB -> resourceA
        shape.getResources().forEach(id -> addBinding(result, shape, id, RelationshipType.RESOURCE));
        // Add all operation BINDING relationships (resource -> operation) and BOUND relations (operation -> resource).
        shape.getAllOperations().forEach(id -> addBinding(result, shape, id, RelationshipType.OPERATION));
        // Do the same, but for all of the lifecycle operations. Note: does not yet another BOUND relationships.
        // READ, UPDATE, DELETE, and PUT are all instance operations by definition
        shape.getRead().ifPresent(id -> {
            result.add(relationship(shape, RelationshipType.READ, id));
            result.add(relationship(shape, RelationshipType.INSTANCE_OPERATION, id));
        });
        shape.getUpdate().ifPresent(id -> {
            result.add(relationship(shape, RelationshipType.UPDATE, id));
            result.add(relationship(shape, RelationshipType.INSTANCE_OPERATION, id));
        });
        shape.getDelete().ifPresent(id -> {
            result.add(relationship(shape, RelationshipType.DELETE, id));
            result.add(relationship(shape, RelationshipType.INSTANCE_OPERATION, id));
        });
        shape.getPut().ifPresent(id -> {
            result.add(relationship(shape, RelationshipType.PUT, id));
            result.add(relationship(shape, RelationshipType.INSTANCE_OPERATION, id));
        });
        // LIST and CREATE are, by definition, collection operations
        shape.getCreate().ifPresent(id -> {
            result.add(relationship(shape, RelationshipType.CREATE, id));
            result.add(relationship(shape, RelationshipType.COLLECTION_OPERATION, id));
        });
        shape.getList().ifPresent(id -> {
            result.add(relationship(shape, RelationshipType.LIST, id));
            result.add(relationship(shape, RelationshipType.COLLECTION_OPERATION, id));
        });
        // Add in all the other collection operations
        shape.getCollectionOperations().forEach(id -> result.add(
                relationship(shape, RelationshipType.COLLECTION_OPERATION, id)));
        // Add in all the other instance operations
        shape.getOperations().forEach(id -> result.add(
                relationship(shape, RelationshipType.INSTANCE_OPERATION, id)));

        // Find resource shapes that bind this resource to it.
        for (ResourceShape resource : model.getResourceShapes()) {
            addServiceAndResourceBindings(result, shape, resource);
        }

        // Find service shapes that bind this resource to it.
        for (ServiceShape service : model.getServiceShapes()) {
            addServiceAndResourceBindings(result, shape, service);
        }

        return result;
    }

    private void addServiceAndResourceBindings(List<Relationship> result, ResourceShape resource, EntityShape entity) {
        if (entity.getResources().contains(resource.getId())) {
            addBinding(result, entity, resource.getId(), RelationshipType.RESOURCE);
        }
    }

    @Override
    public List<Relationship> operationShape(OperationShape shape) {
        List<Relationship> result = new ArrayList<>();
        shape.getInput().ifPresent(id -> result.add(relationship(shape, RelationshipType.INPUT, id)));
        shape.getOutput().ifPresent(id -> result.add(relationship(shape, RelationshipType.OUTPUT, id)));
        for (ShapeId errorId : shape.getErrors()) {
            result.add(relationship(shape, RelationshipType.ERROR, errorId));
        }
        return result;
    }

    @Override
    public List<Relationship> memberShape(MemberShape shape) {
        List<Relationship> result = new ArrayList<>(2);
        result.add(relationship(shape, RelationshipType.MEMBER_CONTAINER, shape.getContainer()));
        result.add(relationship(shape, RelationshipType.MEMBER_TARGET, shape.getTarget()));
        return result;
    }

    @Override
    public List<Relationship> listShape(ListShape shape) {
        return ListUtils.of(relationship(shape, RelationshipType.LIST_MEMBER, shape.getMember()));
    }

    @Override
    public List<Relationship> setShape(SetShape shape) {
        return ListUtils.of(relationship(shape, RelationshipType.SET_MEMBER, shape.getMember()));
    }

    @Override
    public List<Relationship> mapShape(MapShape shape) {
        List<Relationship> result = new ArrayList<>(2);
        result.add(relationship(shape, RelationshipType.MAP_KEY, shape.getKey()));
        result.add(relationship(shape, RelationshipType.MAP_VALUE, shape.getValue()));
        return result;
    }

    @Override
    public List<Relationship> structureShape(StructureShape shape) {
        List<Relationship> result = new ArrayList<>();
        for (ShapeId mixin : shape.getMixins()) {
            result.add(relationship(shape, RelationshipType.MIXIN, mixin));
        }
        for (MemberShape member : shape.getAllMembers().values()) {
            result.add(Relationship.create(shape, RelationshipType.STRUCTURE_MEMBER, member));
        }
        return result;
    }

    @Override
    public List<Relationship> unionShape(UnionShape shape) {
        List<Relationship> result = new ArrayList<>();
        for (ShapeId mixin : shape.getMixins()) {
            result.add(relationship(shape, RelationshipType.MIXIN, mixin));
        }
        for (MemberShape member : shape.getAllMembers().values()) {
            result.add(Relationship.create(shape, RelationshipType.UNION_MEMBER, member));
        }
        return result;
    }

    private Relationship relationship(Shape shape, RelationshipType type, MemberShape memberShape) {
        return Relationship.create(shape, type, memberShape);
    }

    private Relationship relationship(Shape shape, RelationshipType type, ShapeId neighborShapeId) {
        return model.getShape(neighborShapeId)
                .map(target -> Relationship.create(shape, type, target))
                .orElseGet(() -> Relationship.createInvalid(shape, type, neighborShapeId));
    }
}

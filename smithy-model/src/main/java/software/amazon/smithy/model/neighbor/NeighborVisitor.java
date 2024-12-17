/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
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
        return shape.getMixins().isEmpty()
                ? Collections.emptyList()
                : initializeRelationships(shape, 0);
    }

    private List<Relationship> initializeRelationships(Shape shape, int knownMemberCount) {
        if (shape.isMemberShape()) {
            // Members have mixins but shouldn't contribute a relationship.
            return new ArrayList<>(knownMemberCount);
        } else {
            knownMemberCount += shape.getMixins().size();
            List<Relationship> result = new ArrayList<>(knownMemberCount);
            for (ShapeId mixin : shape.getMixins()) {
                push(result, shape, RelationshipType.MIXIN, mixin);
            }
            return result;
        }
    }

    @Override
    public List<Relationship> serviceShape(ServiceShape shape) {
        int neededSize = shape.getOperations().size() + shape.getResources().size() + shape.getErrors().size();
        List<Relationship> result = initializeRelationships(shape, neededSize);

        for (ShapeId operation : shape.getOperations()) {
            push(result, shape, RelationshipType.OPERATION, operation);
        }

        for (ShapeId resource : shape.getResources()) {
            push(result, shape, RelationshipType.RESOURCE, resource);
        }

        for (ShapeId errorId : shape.getErrors()) {
            push(result, shape, RelationshipType.ERROR, errorId);
        }

        return result;
    }

    private void push(List<Relationship> result, Shape container, RelationshipType type, ShapeId bindingTarget) {
        result.add(relationship(container, type, bindingTarget));
    }

    private void push(List<Relationship> result, Shape container, RelationshipType type, MemberShape bindingTarget) {
        result.add(relationship(container, type, bindingTarget));
    }

    @Override
    public List<Relationship> resourceShape(ResourceShape shape) {
        int neededSize = shape.getAllOperations().size() + shape.getResources().size()
                + shape.getIdentifiers().size()
                + shape.getProperties().size();
        List<Relationship> result = initializeRelationships(shape, neededSize);
        shape.getIdentifiers().forEach((k, v) -> push(result, shape, RelationshipType.IDENTIFIER, v));
        shape.getProperties().forEach((k, v) -> push(result, shape, RelationshipType.PROPERTY, v));
        shape.getResources().forEach(id -> push(result, shape, RelationshipType.RESOURCE, id));
        shape.getList().ifPresent(id -> push(result, shape, RelationshipType.LIST, id));
        shape.getCreate().ifPresent(id -> push(result, shape, RelationshipType.CREATE, id));
        shape.getPut().ifPresent(id -> push(result, shape, RelationshipType.PUT, id));
        shape.getRead().ifPresent(id -> push(result, shape, RelationshipType.READ, id));
        shape.getUpdate().ifPresent(id -> push(result, shape, RelationshipType.UPDATE, id));
        shape.getDelete().ifPresent(id -> push(result, shape, RelationshipType.DELETE, id));
        shape.getOperations().forEach(id -> result.add(relationship(shape, RelationshipType.OPERATION, id)));
        shape.getCollectionOperations().forEach(id -> push(result, shape, RelationshipType.COLLECTION_OPERATION, id));
        return result;
    }

    @Override
    public List<Relationship> operationShape(OperationShape shape) {
        ShapeId input = shape.getInput().orElse(null);
        ShapeId output = shape.getOutput().orElse(null);

        // Calculate the number of relationships up front.
        int assumedRelationshipCount = shape.getErrors().size()
                + (input == null ? 0 : 1)
                + (output == null ? 0 : 1);
        List<Relationship> result = initializeRelationships(shape, assumedRelationshipCount);

        if (input != null) {
            push(result, shape, RelationshipType.INPUT, input);
        }

        if (output != null) {
            push(result, shape, RelationshipType.OUTPUT, output);
        }

        for (ShapeId errorId : shape.getErrors()) {
            push(result, shape, RelationshipType.ERROR, errorId);
        }

        return result;
    }

    @Override
    public List<Relationship> memberShape(MemberShape shape) {
        Shape container = model.getShape(shape.getContainer()).orElse(null);

        // Emit a relationship from a member back to the enum, but not from an enum member to Unit.
        boolean isEnumShape = container instanceof EnumShape || container instanceof IntEnumShape;
        List<Relationship> result = initializeRelationships(shape, 1 + (isEnumShape ? 0 : 1));
        push(result, shape, RelationshipType.MEMBER_CONTAINER, shape.getContainer());

        if (!isEnumShape) {
            push(result, shape, RelationshipType.MEMBER_TARGET, shape.getTarget());
        }

        return result;
    }

    @Override
    public List<Relationship> enumShape(EnumShape shape) {
        List<Relationship> result = initializeRelationships(shape, shape.getAllMembers().size());
        for (MemberShape member : shape.getAllMembers().values()) {
            push(result, shape, RelationshipType.ENUM_MEMBER, member);
        }
        return result;
    }

    @Override
    public List<Relationship> intEnumShape(IntEnumShape shape) {
        List<Relationship> result = initializeRelationships(shape, shape.getAllMembers().size());
        for (MemberShape member : shape.getAllMembers().values()) {
            push(result, shape, RelationshipType.INT_ENUM_MEMBER, member);
        }
        return result;
    }

    @Override
    public List<Relationship> listShape(ListShape shape) {
        List<Relationship> result = initializeRelationships(shape, 1);
        push(result, shape, RelationshipType.LIST_MEMBER, shape.getMember());
        return result;
    }

    @Override
    public List<Relationship> setShape(SetShape shape) {
        List<Relationship> result = initializeRelationships(shape, 1);
        push(result, shape, RelationshipType.SET_MEMBER, shape.getMember());
        return result;
    }

    @Override
    public List<Relationship> mapShape(MapShape shape) {
        List<Relationship> result = initializeRelationships(shape, 2);
        push(result, shape, RelationshipType.MAP_KEY, shape.getKey());
        push(result, shape, RelationshipType.MAP_VALUE, shape.getValue());
        return result;
    }

    @Override
    public List<Relationship> structureShape(StructureShape shape) {
        List<Relationship> result = initializeRelationships(shape, shape.getAllMembers().size());
        for (MemberShape member : shape.getAllMembers().values()) {
            push(result, shape, RelationshipType.STRUCTURE_MEMBER, member);
        }
        return result;
    }

    @Override
    public List<Relationship> unionShape(UnionShape shape) {
        List<Relationship> result = initializeRelationships(shape, shape.getAllMembers().size());
        for (MemberShape member : shape.getAllMembers().values()) {
            push(result, shape, RelationshipType.UNION_MEMBER, member);
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

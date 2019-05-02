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

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Represent a direct relationship between two shapes.
 *
 * <p>A relationship is a connection between two shapes. See
 * {@link RelationshipType} for documentation on the possible
 * types of relationships.
 */
public final class Relationship {
    private final Shape shape;
    private final RelationshipType relationshipType;
    private final ShapeId neighborShapeId;
    private final Shape neighborShape;

    /**
     * Constructs a shape relationship where the neighbor is not present.
     *
     * @param shape The shape the relationship originates from.
     * @param relationshipType The relationshipType of relationship.
     * @param neighborShapeId The id of the missing shape the relationship targets.
     */
    public Relationship(Shape shape, RelationshipType relationshipType, ShapeId neighborShapeId) {
        this(shape, relationshipType, neighborShapeId, null);
    }

    /**
     * Constructs a shape relationship where the neighbor is present.
     *
     * @param shape The shape the relationship originates from.
     * @param relationshipType The relationshipType of relationship.
     * @param neighborShape The shape the relationship targets.
     */
    public Relationship(Shape shape, RelationshipType relationshipType, Shape neighborShape) {
        this(
                shape,
                relationshipType,
                Objects.requireNonNull(neighborShape, "Neighbor shape must not be null").getId(),
                shape
        );
    }

    /**
     * Constructs a shape relationship.
     *
     * <p>Describes a relationship between two shapes. Relationships are
     * directional. The starting shape references the neighbor shape. The
     * relationshipType of relationship is given as a {@link RelationshipType}.
     * If when building this relationship, the neighbor shape is not present
     * in the shape index, then the {@code neighborShape} argument can be
     * null. When present, the {@code neighborShapeId} MUST match the ID of
     * the given {@code neighborShape}.
     *
     * @param shape The shape the relationship originates from.
     * @param relationshipType The relationshipType of relationship.
     * @param neighborShapeId The id of the shape the relationship targets.
     * @param neighborShape The nullable member shape.
     */
    public Relationship(
            Shape shape,
            RelationshipType relationshipType,
            ShapeId neighborShapeId,
            Shape neighborShape
    ) {
        this.shape = Objects.requireNonNull(shape);
        this.relationshipType = Objects.requireNonNull(relationshipType);
        this.neighborShapeId = Objects.requireNonNull(neighborShapeId);
        this.neighborShape = neighborShape;

        if (neighborShape != null && !neighborShapeId.equals(neighborShape.getId())) {
            throw new IllegalArgumentException("neighborShapeId must be the same as neighborShape#getId()");
        }
    }

    /**
     * Gets the starting shape in the relationship.
     *
     * @return Returns the shape the relationship is from.
     */
    public Shape getShape() {
        return shape;
    }

    /**
     * Gets the relationship type.
     *
     * @return Returns the relationship type.
     */
    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    /**
     * Gets the shape id of the neighbor shape.
     *
     * @return Returns the shape id of the neighbor shape.
     */
    public ShapeId getNeighborShapeId() {
        return neighborShapeId;
    }

    /**
     * Gets the optional neighbor shape; the neighbor shape may be empty
     * when the neighbor shape id was not in the shape index.
     *
     * @return Returns the optional neighbor shape.
     */
    public Optional<Shape> getNeighborShape() {
        return Optional.ofNullable(neighborShape);
    }

    /**
     * Gets the neighbor shape or throws if it doesn't exist.
     *
     * @return Returns the neighbor shape.
     * @throws ExpectationNotMetException if the neighbor is missing.
     */
    public Shape expectNeighborShape() {
        if (neighborShape == null) {
            throw new ExpectationNotMetException("Neighbor does not exist: " + neighborShapeId, shape);
        }

        return neighborShape;
    }

    @Override
    public String toString() {
        return String.format("[Relationship shape=\"%s\" type=\"%s\" neighbor=\"%s\" neighborPresent=%b]",
                shape.getId(), relationshipType, neighborShapeId, neighborShape != null);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Relationship)) {
            return false;
        }
        Relationship otherRelationship = (Relationship) other;
        return shape.equals(otherRelationship.shape)
                && relationshipType.equals(otherRelationship.relationshipType)
                && neighborShapeId.equals(otherRelationship.neighborShapeId)
                && Objects.equals(neighborShape, otherRelationship.neighborShape);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shape, relationshipType, neighborShapeId);
    }
}

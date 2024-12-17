/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.selector.Selector;
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

    private Relationship(
            Shape shape,
            RelationshipType relationshipType,
            ShapeId neighborShapeId,
            Shape neighborShape
    ) {
        this.shape = Objects.requireNonNull(shape);
        this.relationshipType = Objects.requireNonNull(relationshipType);
        this.neighborShapeId = Objects.requireNonNull(neighborShapeId);
        this.neighborShape = neighborShape;
    }

    /**
     * Constructs a valid shape relationship where the neighbor is present.
     *
     * @param shape The shape the relationship originates from.
     * @param relationshipType The relationshipType of relationship.
     * @param neighborShape The shape the relationship targets.
     * @return Returns the created Relationship.
     */
    public static Relationship create(Shape shape, RelationshipType relationshipType, Shape neighborShape) {
        return new Relationship(shape, relationshipType, neighborShape.getId(), neighborShape);
    }

    /**
     * Constructs an invalid shape relationship where the neighbor is not present.
     *
     * @param shape The shape the relationship originates from.
     * @param relationshipType The relationshipType of relationship.
     * @param neighborShapeId The shape the relationship targets.
     * @return Returns the created Relationship.
     */
    public static Relationship createInvalid(Shape shape, RelationshipType relationshipType, ShapeId neighborShapeId) {
        return new Relationship(shape, relationshipType, neighborShapeId, null);
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
     * when the neighbor shape id was not in the model.
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

    /**
     * Gets the token that is used in {@link Selector} expressions when
     * referring to the relationship or an empty {@code Optional} if this
     * relationship is not used in a selector.
     *
     * @return Returns the optionally present selector token for this relationship.
     */
    public Optional<String> getSelectorLabel() {
        return relationshipType.getSelectorLabel();
    }

    /**
     * Gets the direction of the relationship.
     *
     * <p>A {@link RelationshipDirection#DIRECTED} direction is formed from a shape
     * that defines a reference to another shape (for example, when a resource
     * defines operations or resources it contains).
     *
     * <p>A {@link RelationshipDirection#INVERTED} relationship is a relationship
     * from a shape to a shape that defines a relationship to it. The target
     * of such a relationship doesn't define the relationship, but is the
     * target of the relationship.
     *
     * @return Returns the direction of the relationship.
     */
    public RelationshipDirection getDirection() {
        return relationshipType.getDirection();
    }

    @Override
    public String toString() {
        return String.format("[Relationship shape=\"%s\" type=\"%s\" neighbor=\"%s\" neighborPresent=%b]",
                shape.getId(),
                relationshipType,
                neighborShapeId,
                neighborShape != null);
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

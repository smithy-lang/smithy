/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

/**
 * Defines the directionality of a relationship.
 */
public enum RelationshipDirection {
    /**
     * A directed relationship that goes from a shape to a shape that it
     * references. For example, a {@link RelationshipType#MEMBER_TARGET}
     * is a directed relationship.
     */
    DIRECTED,

    /**
     * The relationship goes from a shape to the shape that defines a
     * directed relationship to the shape. For example, a
     * {@link RelationshipType#BOUND} relation is an inverted relationship.
     */
    INVERTED,
}

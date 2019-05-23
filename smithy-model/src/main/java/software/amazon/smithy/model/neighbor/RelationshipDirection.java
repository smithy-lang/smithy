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

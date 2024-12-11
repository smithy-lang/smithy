/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Creates traits from {@link Node} values.
 *
 * <p>This is the interface used to create traits when loading a model.
 * If a trait implementation does not have a corresponding
 * {@link TraitService}, the concrete class for the trait will not be
 * used in code, and instead a {@link DynamicTrait} trait will be used.
 */
public interface TraitService {
    /**
     * @return Gets the shape ID of the trait that this provider created.
     */
    ShapeId getShapeId();

    /**
     * Creates the trait from a node value.
     *
     * @param target The shape targeted by the trait.
     * @param value The value of the trait.
     * @return Returns the created trait.
     */
    Trait createTrait(ShapeId target, Node value);
}

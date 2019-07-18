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

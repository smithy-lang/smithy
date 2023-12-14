/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.traitcodegen.generators.traits.TraitGenerator;

/**
 * Provides {@link TraitGenerator} objects for shapes.
 *
 * <p>Implementations of this interface are used to determine what
 * generator to use for generating a trait class from a shape.
 *
 */
public interface TraitGeneratorProvider {
    /**
     * Gets a trait generator to use for creating the given shape.
     *
     * @param shape shape to get generator for
     * @return trait generator object to use for writing trait class contents.
     */
    TraitGenerator getGenerator(Shape shape);
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.util.function.Consumer;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Provides a generator object that consumes the trait codegen context and generates a
 * java shape definition.
 *
 * <p>Implementations of this interface are used to determine what
 * generator to use for generating a trait class from a shape.
 *
 */
public interface GeneratorProvider {
    /**
     * Gets a trait generator to use for creating the given shape.
     *
     * @param shape shape to get generator for
     * @return trait generator object to use for writing trait class contents.
     */
    Consumer<GenerateTraitDirective> getGenerator(Shape shape);
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.traitcodegen.generators.traits.TraitGenerator;

public interface TraitGeneratorProvider {
    TraitGenerator getGenerator(Shape shape);
}

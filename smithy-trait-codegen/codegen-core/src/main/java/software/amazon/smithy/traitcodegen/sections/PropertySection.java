/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.sections;

import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.CodeSection;

public final class PropertySection implements CodeSection {
    private final Shape shape;

    public PropertySection(Shape shape) {
        this.shape = shape;
    }

    public Shape shape() {
        return shape;
    }
}

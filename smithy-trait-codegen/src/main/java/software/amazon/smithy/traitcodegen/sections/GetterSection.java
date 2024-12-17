/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.sections;

import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.CodeSection;

/**
 * Contains a getter method.
 */
public final class GetterSection implements CodeSection {
    private final Shape shape;

    public GetterSection(Shape shape) {
        this.shape = shape;
    }

    /**
     * {@link Shape} that this getter returns a Java object for.
     */
    public Shape shape() {
        return shape;
    }
}

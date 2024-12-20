/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.sections;

import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.CodeSection;

/**
 * Contains a Java doc section attached to a class or method.
 */
public final class JavaDocSection implements CodeSection {
    private final Shape shape;

    public JavaDocSection(Shape shape) {
        this.shape = shape;
    }

    /**
     * {@link Shape} that the class the Javadoc is added to represents.
     */
    public Shape shape() {
        return shape;
    }
}

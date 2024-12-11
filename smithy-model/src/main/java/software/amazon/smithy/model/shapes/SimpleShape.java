/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

/**
 * Abstract class representing all simple type shapes.
 */
public abstract class SimpleShape extends Shape {
    SimpleShape(AbstractShapeBuilder<?, ? extends SimpleShape> builder) {
        super(builder, false);
    }
}

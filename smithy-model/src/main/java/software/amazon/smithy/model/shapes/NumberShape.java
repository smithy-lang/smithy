/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

/**
 * Abstract class representing all numeric types.
 */
public abstract class NumberShape extends SimpleShape {
    NumberShape(AbstractShapeBuilder<?, ? extends NumberShape> builder) {
        super(builder);
    }
}

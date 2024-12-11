/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

/**
 * Interface used to convert a shape to a ShapeId.
 */
public interface ToShapeId {

    /**
     * @return Returns the shape ID of an object.
     */
    ShapeId toShapeId();
}

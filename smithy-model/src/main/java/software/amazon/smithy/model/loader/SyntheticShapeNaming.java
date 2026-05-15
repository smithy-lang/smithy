/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Generates deterministic names for synthetic shapes created from inline
 * collection declarations.
 */
final class SyntheticShapeNaming {

    private static final String PREFIX = "_Synthetic";

    private SyntheticShapeNaming() {}

    /**
     * Generates a synthetic name for an inline list shape.
     *
     * @param memberTarget The shape ID of the list member's target.
     * @return The synthetic shape name (without namespace).
     */
    static String listName(ShapeId memberTarget) {
        return PREFIX + "ListOf" + memberTarget.getName();
    }

    /**
     * Generates a synthetic name for an inline map shape.
     *
     * @param keyTarget The shape ID of the map key's target.
     * @param valueTarget The shape ID of the map value's target.
     * @return The synthetic shape name (without namespace).
     */
    static String mapName(ShapeId keyTarget, ShapeId valueTarget) {
        return PREFIX + "MapOf" + keyTarget.getName() + "To" + valueTarget.getName();
    }
}

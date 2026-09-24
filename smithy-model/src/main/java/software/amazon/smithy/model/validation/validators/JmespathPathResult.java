/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The result of {@link JmespathShapeWalker#walk} walking a structural JMESPath over a starting shape.
 */
final class JmespathPathResult {
    // The leaf value shape the path resolves to, with array levels unwrapped, or null on error.
    final Shape leaf;
    // True if the path is the bare current node (resolves to the starting shape/root).
    final boolean root;
    // The ordered identities of the lists the path iterates through (outermost first), forming a
    // cardinality signature. Two paths correlate element-for-element iff their signatures are equal.
    // Empty for a scalar (non-array) leaf.
    final List<ShapeId> arrays;
    // Number of array levels (projections or flattens) traversed; equal to arrays.size().
    final int arrayDepth;
    // Non-null when a segment could not be resolved against the model.
    final String error;

    private JmespathPathResult(Shape leaf, boolean root, List<ShapeId> arrays, String error) {
        this.leaf = leaf;
        this.root = root;
        this.arrays = arrays == null ? Collections.emptyList() : Collections.unmodifiableList(arrays);
        this.arrayDepth = this.arrays.size();
        this.error = error;
    }

    static JmespathPathResult of(Shape leaf, boolean root, List<ShapeId> arrays) {
        return new JmespathPathResult(leaf, root, arrays, null);
    }

    static JmespathPathResult error(String error) {
        return new JmespathPathResult(null, false, null, error);
    }
}

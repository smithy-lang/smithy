/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.diff.evaluators.configurable;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;

/**
 * Possible values for the {@code appliesTo} property of configurable diff evaluators.
 *
 * <p>This property configures which model the selectors should run on.
 * <ol>
 *     <li> Using {@link AppliesTo#ADDED_SHAPES} will configure the evaluator to run selectors on
 *          the new model, and only consider matches corresponding to added shapes.
 *     <li> Using {@link AppliesTo#REMOVED_SHAPES} will configure the evaluator to run selectors on
 *          the old model, and only consider matches corresponding to removed shapes.
 * </ol>
 */
enum AppliesTo {
    ADDED_SHAPES("AddedShapes"),
    REMOVED_SHAPES("RemovedShapes");

    private final String stringValue;

    AppliesTo(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    static AppliesTo fromNode(Node node) {
        return new NodeMapper().deserialize(node, AppliesTo.class);
    }
}

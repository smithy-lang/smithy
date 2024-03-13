/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.diff.evaluators.configurable;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;

/**
 * The possible values of the {@code emitCondition} property of configurable diff evaluators.
 *
 * <p>This property configures the conditions under which the diff evaluator should emit events,
 * based on the shapes which matched the configured {@code selector}. These conditions are applied
 * to the set of shapes configured by {@code appliesTo} and {@code filter} (if present).
 * <ol>
 *     <li>Using {@link EmitCondition#IF_ANY_MATCH} will emit a single event if any of the shapes match.
 *     <li>Using {@link EmitCondition#IF_ALL_MATCH} will emit a single event if all the shapes match.
 *     <li>Using {@link EmitCondition#IF_NONE_MATCH} will emit a single event if none of the shapes match.
 *     <li>Using {@link EmitCondition#FOR_EACH_MATCH} will emit an event for each shape that matches.
 * </ol>
 */
enum EmitCondition {
    IF_ANY_MATCH("IfAnyMatch"),
    IF_ALL_MATCH("IfAllMatch"),
    IF_NONE_MATCH("IfNoneMatch"),
    FOR_EACH_MATCH("ForEachMatch");

    private final String stringValue;

    EmitCondition(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    static EmitCondition fromNode(Node node) {
        return new NodeMapper().deserialize(node, EmitCondition.class);
    }
}

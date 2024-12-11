/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

/**
 * Shapes order for code generation.
 *
 * <p>CodegenDirector order the shapes appropriately before feeding them to the code generators. See {@link
 * software.amazon.smithy.codegen.core.directed.CodegenDirector#shapeGenerationOrder(ShapeGenerationOrder)}
 */
public enum ShapeGenerationOrder {
    /**
     * Shapes ordered in reverse-topological order. Also see {@link TopologicalIndex}
     */
    TOPOLOGICAL,

    /**
     * Shapes ordered alphabetically by their names.
     */
    ALPHABETICAL,

    /**
     * Shapes without order.
     */
    NONE
}

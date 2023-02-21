/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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

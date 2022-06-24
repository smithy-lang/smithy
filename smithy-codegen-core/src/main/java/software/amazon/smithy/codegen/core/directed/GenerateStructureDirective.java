/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Directive used to generate a structure.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateStructure
 */
public final class GenerateStructureDirective<C extends CodegenContext<S, ?, ?>, S>
        extends ShapeDirective<StructureShape, C, S> {

    GenerateStructureDirective(C context, ServiceShape service, StructureShape shape) {
        super(context, service, shape);
    }

    /**
     * Check if this is a shape used exclusively for input.
     *
     * <p>This is equivalent to calling {@code shape().hasTrait(InputTrait.class)}.
     *
     * <p>Use the {@link CodegenDirector#createDedicatedInputsAndOutputs()} method
     * to ensure that every operation has a unique input shape marked with the
     * input trait.
     *
     * @return Returns true if the shape is marked with the {@code input} trait.
     * @see ModelTransformer#createDedicatedInputAndOutput
     */
    public boolean isInputShape() {
        return shape().hasTrait(InputTrait.class);
    }

    /**
     * Check if this is a shape used exclusively for output.
     *
     * <p>This is equivalent to calling {@code shape().hasTrait(OutputTrait.class)}.
     *
     * <p>Use the {@link CodegenDirector#createDedicatedInputsAndOutputs()} method
     * to ensure that every operation has a unique output shape marked with the
     * output trait.
     *
     * @return Returns true if the shape is marked with the {@code output} trait.
     * @see ModelTransformer#createDedicatedInputAndOutput
     */
    public boolean isOutputShape() {
        return shape().hasTrait(OutputTrait.class);
    }
}

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
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Directive used to generate a union.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @param <D> WriterDelegator type.
 * @see DirectedCodegen#generateUnion
 */
public final class GenerateUnion<C extends CodegenContext<S>, S, D extends WriterDelegator<?>>
        extends ShapeDirective<UnionShape, C, S, D> {

    GenerateUnion(C context, ServiceShape service, D writerDelegator, UnionShape shape) {
        super(context, service, writerDelegator, shape);
    }

    /**
     * Check if this is an event stream union.
     *
     * @return Returns true if this is an event stream.
     */
    public boolean isEventStream() {
        return shape().hasTrait(StreamingTrait.class);
    }
}

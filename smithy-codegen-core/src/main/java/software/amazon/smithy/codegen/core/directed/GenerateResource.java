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
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive used to generate a resource.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @param <D> WriterDelegator type.
 * @see DirectedCodegen#generateResource
 */
public final class GenerateResource<C extends CodegenContext<S>, S, D extends WriterDelegator<?>>
        extends ShapeDirective<ResourceShape, C, S, D> {

    GenerateResource(C context, ServiceShape service, D writerDelegator, ResourceShape shape) {
        super(context, service, writerDelegator, shape);
    }
}

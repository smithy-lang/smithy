/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core.writer;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.CodeWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An observer invoked when a shape CodegenWriter is used.
 *
 * <p>The following example defines a {@code UseShapeWriterObserver} that
 * writes a comment before a shape is written:
 *
 * <pre>{@code
 * public final class MyObserver implements UseShapeWriterObserver<MyWriter> {
 *     \@Override
 *     public void observe(Shape shape, Symbol symbol, SymbolProvider symbolProvider, MyWriter writer) {
 *         writer.write("/// Writing $L", shape.getId());
 *     }
 * }
 * }</pre>
 *
 * @param <T> Type of CodegenWriter being used.
 */
@FunctionalInterface
@SmithyUnstableApi
public interface UseShapeWriterObserver<T extends CodegenWriter<T, ?>> {
    /**
     * Invoked when a {@link CodegenWriter} writer is used via
     * {@link CodegenWriterDelegator#useShapeWriter(Shape, Consumer)}.
     *
     * <p>This is an extension point that allows code generators to perform
     * any kind of preprocessing they need before code is written to a
     * {@link CodegenWriter} for a given shape. For example, this could be
     * used to add comments to the generated code to indicate that a file is
     * auto-generated.
     *
     * <p>This method is invoked before the {@code writerConsumer} of
     * {@link CodegenWriterDelegator#useShapeWriter} is called. This method
     * is invoked within a pushed CodegenWriter state, so any state
     * modifications made to the CodegenWriter will not persist after the the
     * {@code writerConsumer} has completed (e.g., calls to methods like
     * {@link CodeWriter#indent} are not persisted).
     *
     * @param shape          Shape being used.
     * @param symbol         Symbol of the shape.
     * @param symbolProvider SymbolProvider associated with the delegator.
     * @param writer         Writer being used for the shape.
     */
    void observe(Shape shape, Symbol symbol, SymbolProvider symbolProvider, T writer);
}

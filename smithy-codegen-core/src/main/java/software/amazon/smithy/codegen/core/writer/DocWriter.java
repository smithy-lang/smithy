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

import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.utils.AbstractCodeWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * <p>This class is deprecated and will be removed in a future release.
 *
 * <p>Use {@link SymbolWriter} directly instead.
 *
 * Responsible for properly writing documentation emitted when a
 * {@code Runnable} in invoked.
 *
 * <p>The following example shows how to implement a basic
 * {@code DocumentationWriter} that encloses documentation in
 * successive lines that start with "///".
 *
 * <pre>{@code
 * public final class MyDocWriter implements DocumentationWriter<MyWriter> {
 *     \@Override
 *     public void writeDocs(T writer, Runnable runnable) {
 *         setNewlinePrefix("/// ")
 *         runnable.run();
 *     }
 * }
 * }</pre>
 *
 * @param <T> The type of {@code AbstractCodeWriter} being written to.
 */
@FunctionalInterface
@SmithyUnstableApi
@Deprecated
public interface DocWriter<T extends AbstractCodeWriter<T>> {

    /**
     * Writes documentation comments.
     *
     * <p>Implementations are expected to write out the beginning of a documentation
     * comment, set any necessary prefix for each line written while writing docs,
     * then invoke the given {@code runnable}, then finally write the closing
     * characters for documentation.
     *
     * @param writer   Writer to configure for writing documentation.
     * @param runnable Runnable that handles actually writing docs with the writer.
     */
    void writeDocs(T writer, Runnable runnable);
}

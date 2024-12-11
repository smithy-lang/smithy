/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.writer;

import software.amazon.smithy.utils.CodeWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
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
 * @param <T> The type of {@code CodegenWriter} being written to.
 * @deprecated prefer {@link DocWriter}. This will be removed in a future release.
 */
@Deprecated
@FunctionalInterface
@SmithyUnstableApi
public interface DocumentationWriter<T extends CodeWriter> {
    // Implementer's note: this class is not tied to CodegenWriter; it can be
    // used with any kind of CodeWriter, allowing any kind of CodegenWriters to
    // be used but also making this type more general-purpose.

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

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.writer;

import java.util.function.BiFunction;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * <p>Factory used to create a {@code CodegenWriter}.
 *
 * <p>The following example shows how to implement a basic
 * {@code CodegenWriterFactory}.
 *
 * <pre>{@code
 * public final class MyWriterFactory implements CodegenWriterFactory<MyWriter> {
 *     \@Override
 *     public MyWriter apply(String filename, String namespace) {
 *         return new MyWriter(namespace);
 *     }
 * }
 * }</pre>
 *
 * <p>Because {@code CodegenWriterFactory} is a {@link FunctionalInterface},
 * it can be implemented using a lambda expression:
 *
 * <pre>{@code
 * CodegenWriterFactory<MyWriter> = (filename, namespace) -> new MyWriter(namespace);
 * }</pre>
 *
 * @param <T> Type of {@code CodegenWriter} to create.
 * @deprecated prefer {@link software.amazon.smithy.codegen.core.SymbolWriter.Factory}.
 * This will be removed in a future release.
 */
@FunctionalInterface
@SmithyUnstableApi
@Deprecated
public interface CodegenWriterFactory<T extends CodegenWriter<T, ?>> extends BiFunction<String, String, T> {
    /**
     * Creates a {@code CodegenWriter} of type {@code T} for the given
     * filename and namespace.
     *
     * @param filename  Non-null filename of the writer being created.
     * @param namespace Non-null namespace associated with the file (possibly empty string).
     * @return Returns the created writer of type {@code T}.
     */
    T apply(String filename, String namespace);
}

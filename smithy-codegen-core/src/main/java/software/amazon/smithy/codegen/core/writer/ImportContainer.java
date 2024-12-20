/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.writer;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.utils.CodeWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains the imports associated with a specific file.
 *
 * <p>This class is deprecated and will be removed in a future release.
 *
 * <p>Use {@link software.amazon.smithy.codegen.core.ImportContainer} instead.
 */
@SmithyUnstableApi
@Deprecated
public interface ImportContainer {
    /**
     * Adds an import for the given symbol if and only if the "namespace" of the
     * provided Symbol differs from the "namespace" associated with the
     * ImportContainer.
     *
     * <p>"namespace" in this context can mean whatever it needs to mean for the
     * target programming language. In some languages, it might mean the path to
     * a file. In others, it might mean a proper namespace string. It's up to
     * subclasses to both track a current "namespace" and implement this method
     * in a way that makes sense.
     *
     * @param symbol Symbol to import if it's in another namespace.
     * @param alias  Alias to import the symbol as.
     */
    void importSymbol(Symbol symbol, String alias);

    /**
     * Implementations must implement a custom {@code toString} method that
     * converts the collected imports to code that can be written to a
     * {@link CodeWriter}.
     *
     * @return Returns the collected imports as a string.
     */
    String toString();
}

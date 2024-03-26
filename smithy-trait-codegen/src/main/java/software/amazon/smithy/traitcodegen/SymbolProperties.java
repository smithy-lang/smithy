/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class SymbolProperties {
    /**
     *  Provides an initializer for the builder ref.
     *
     *  <p>This should always be included on collection shapes such as Maps and Lists.
     */
    public static final String BUILDER_REF_INITIALIZER = "builder-ref-initializer";

    /**
     * The "base" symbol for a trait.
     *
     * <p>This is the symbol that the shape would resolve
     * to if it were not marked with `@trait`. This property is expected on all
     * trait symbols.
     */
    public static final String BASE_SYMBOL = "base-symbol";

    /**
     * The unboxed or primitive version of a Symbol.
     *
     * <p>This property should be included on symbols such as `Integer` that
     * have the boxed version {@code Integer} and an unboxed (primitive) version
     * {@code integer}.
     */
    public static final String UNBOXED_SYMBOL = "unboxed-symbol";

    /**
     * Indicates that the given symbol is a primitive type.
     *
     * <p>This property is checked for existence only and should have no meaningful value.
     */
    public static final String IS_PRIMITIVE = "primitive";

    private SymbolProperties() {
        // No constructor for constants class
    }
}

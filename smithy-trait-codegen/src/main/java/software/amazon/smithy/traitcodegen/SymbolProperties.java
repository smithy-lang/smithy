/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.codegen.core.Property;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class SymbolProperties {
    /**
     *  Provides an initializer for the builder ref.
     *
     *  <p>This should always be included on collection shapes such as Maps and Lists.
     */
    public static final Property<String> BUILDER_REF_INITIALIZER = Property.named("builder-ref-initializer");

    /**
     * The "base" symbol for a trait.
     *
     * <p>This is the symbol that the shape would resolve
     * to if it were not marked with `@trait`. This property is expected on all
     * trait symbols.
     */
    public static final Property<Symbol> BASE_SYMBOL = Property.named("base-symbol");

    /**
     * The unboxed or primitive version of a Symbol.
     *
     * <p>This property should be included on symbols such as `Integer` that
     * have the boxed version {@code Integer} and an unboxed (primitive) version
     * {@code integer}.
     */
    public static final Property<Symbol> UNBOXED_SYMBOL = Property.named("unboxed-symbol");

    /**
     * Indicates that the given symbol is a primitive type.
     *
     * <p>This property is checked for existence only and should have no meaningful value.
     */
    public static final Property<Boolean> IS_PRIMITIVE = Property.named("primitive");

    private SymbolProperties() {
        // No constructor for constants class
    }
}

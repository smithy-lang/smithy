/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class SymbolProperties {
    // Provides an initializer for the builder ref. This should always
    // be included on collection shapes such as Maps and Lists
    public static final String BUILDER_REF_INITIALIZER = "builder-ref-initializer";

    // The "base" symbol for a trait. This is the symbol that the shape would resolve
    // to if it were not marked with `@trait`. This property is expected on all
    // trait symbols.
    public static final String BASE_SYMBOL = "base-symbol";

    private SymbolProperties() {
        // No constructor for constants class
    }
}

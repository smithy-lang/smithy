/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.sections;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.utils.CodeSection;

/**
 * Contains a {@code toBuilder} method.
 */
public final class ToBuilderSection implements CodeSection {
    private final Symbol symbol;

    public ToBuilderSection(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol symbol() {
        return symbol;
    }
}

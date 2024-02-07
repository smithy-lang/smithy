/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.sections;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.utils.CodeSection;

/**
 * Contains a {@code FromNode} method definition.
 */
public final class FromNodeSection implements CodeSection {
    private final Symbol symbol;

    public FromNodeSection(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol symbol() {
        return symbol;
    }
}

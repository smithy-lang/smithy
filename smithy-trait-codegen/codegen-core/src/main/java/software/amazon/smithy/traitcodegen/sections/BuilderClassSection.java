/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.sections;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.utils.CodeSection;

/**
 * Contains the nested builder class for a trait.
 */
public final class BuilderClassSection implements CodeSection {
    private final Symbol symbol;

    public BuilderClassSection(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol symbol() {
        return symbol;
    }
}

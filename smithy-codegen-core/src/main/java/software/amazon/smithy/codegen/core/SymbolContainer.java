/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.List;

/**
 * A holder for {@link Symbol} objects.
 */
public interface SymbolContainer {
    /**
     * Returns any {@link Symbol} objects contained in the object.
     *
     * @return Returns a collection of {@code Symbol}s.
     */
    List<Symbol> getSymbols();
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.Symbol;

final class TestImports implements ImportContainer {
    Map<String, Symbol> imports = new TreeMap<>();

    @Override
    public void importSymbol(Symbol symbol, String alias) {
        imports.put(alias, symbol);
    }
}

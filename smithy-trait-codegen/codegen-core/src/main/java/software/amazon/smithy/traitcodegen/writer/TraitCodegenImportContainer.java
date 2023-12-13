/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.writer;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.Symbol;

final class TraitCodegenImportContainer implements ImportContainer {
    private static final String JAVA_NAMESPACE_PREFIX = "java.lang";
    private final Set<Symbol> imports = new HashSet<>();

    @Override
    public void importSymbol(Symbol symbol, String alias) {
        if (!symbol.getNamespace().startsWith(JAVA_NAMESPACE_PREFIX)) {
            imports.add(symbol);
        }
    }

    @Override
    public String toString() {
        Set<String> sortedImports = imports.stream().map(Symbol::getFullName)
                .collect(Collectors.toCollection(TreeSet::new));
        StringBuilder builder = new StringBuilder();
        for (String importName : sortedImports) {
            builder.append("import ");
            builder.append(importName);
            builder.append(";");
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }
}

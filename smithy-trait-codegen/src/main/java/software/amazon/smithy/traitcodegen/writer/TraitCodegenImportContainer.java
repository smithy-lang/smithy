/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.writer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.traitcodegen.SymbolProperties;

/**
 * Import container for Java imports.
 */
final class TraitCodegenImportContainer implements ImportContainer {
    private final Map<String, Set<Symbol>> imports = new HashMap<>();
    private final String namespace;

    TraitCodegenImportContainer(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public void importSymbol(Symbol symbol, String alias) {
        // Do not import primitive types
        if (symbol.getProperty(SymbolProperties.IS_PRIMITIVE).isPresent()) {
            return;
        }
        Set<Symbol> duplicates = imports.computeIfAbsent(symbol.getName(), sn -> new HashSet<>());
        duplicates.add(symbol);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String importName : getSortedAndFilteredImports()) {
            builder.append("import ").append(importName).append(";");
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    /**
     * Sort imports then filter out any instances of duplicates. Then filter out and instances of base java classes
     * that do not need to be imported. Finally, filter out cases where the symbol has the same namespace as the file.
     *
     * @return sorted list of imports
     */
    private Set<String> getSortedAndFilteredImports() {
        return imports.values()
                .stream()
                .filter(s -> s.size() == 1)
                .map(s -> s.iterator().next())
                .filter(s -> !s.getNamespace().startsWith("java.lang"))
                .filter(s -> !s.getNamespace().equals(namespace))
                .map(Symbol::getFullName)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}

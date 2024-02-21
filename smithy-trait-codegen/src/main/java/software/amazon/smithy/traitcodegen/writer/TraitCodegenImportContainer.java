/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.writer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.utils.StringUtils;

/**
 * Import container for Java imports.
 */
final class TraitCodegenImportContainer implements ImportContainer {
    private static final String JAVA_NAMESPACE_PREFIX = "java.lang";
    private final Map<String, Set<String>> imports = new HashMap<>();
    // TODO: Add builder here?
    private final String namespace;
    private final String className;

    TraitCodegenImportContainer(String namespace, String fileName) {
        this.namespace = namespace;
        this.className = extractClassName(fileName);
        System.out.println("CLASS NAME: " + className);
    }

    @Override
    public void importSymbol(Symbol symbol, String alias) {
        // Do not import the symbol if it is in the base java namespace,
        // is in the same namespace as the file, or has the same name as the current class.
        if (!symbol.getNamespace().startsWith(JAVA_NAMESPACE_PREFIX)
                && !symbol.getNamespace().equals(namespace)
                && !symbol.getName().equals(className)
        ) {
            Set<String> duplicates = imports.computeIfAbsent(symbol.getName(), sn -> new HashSet<>());
            duplicates.add(symbol.getFullName());
        }
    }

    @Override
    public String toString() {
        // Sort imports and filter out any instances of duplicates
        Set<String> sortedImports = imports.values().stream()
                .filter(s -> s.size() == 1)
                .map(s -> s.iterator().next())
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

    private String extractClassName(String filename) {
        return StringUtils.strip(filename, ".java").substring(filename.lastIndexOf("/") + 1);
    }
}

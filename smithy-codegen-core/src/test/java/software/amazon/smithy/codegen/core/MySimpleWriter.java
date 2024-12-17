/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.Map;
import java.util.TreeMap;

/**
 * A pretty basic implementation of SymbolWriter.
 */
final class MySimpleWriter extends SymbolWriter<MySimpleWriter, MySimpleWriter.MyImportContainer> {

    public MySimpleWriter(String namespace) {
        super(new MyImportContainer(namespace));
    }

    static final class MyImportContainer implements ImportContainer {
        public final Map<String, String> imports = new TreeMap<>();
        private final String namespace;

        private MyImportContainer(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public void importSymbol(Symbol symbol, String alias) {
            if (!symbol.getNamespace().equals(namespace)) {
                imports.put(alias, symbol.toString());
            }
        }
    }
}

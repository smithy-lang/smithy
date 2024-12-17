/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.writer;

import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.codegen.core.Symbol;

/**
 * A pretty basic implementation of CodegenWriter.
 */
final class MySimpleWriter extends SymbolWriter<MySimpleWriter, MySimpleWriter.MyImportContainer> {

    public MySimpleWriter(String namespace) {
        super(new TestDocumentationWriter(), new MyImportContainer(namespace));
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

    static final class TestDocumentationWriter implements DocWriter<MySimpleWriter> {
        @Override
        public void writeDocs(MySimpleWriter writer, Runnable runnable) {
            writer.pushFilteredState(this::sanitizeDocString);
            writer.write("Before");
            runnable.run();
            writer.write("After");
            writer.popState();
        }

        private String sanitizeDocString(String docs) {
            return docs.replace("!", "!!");
        }
    }
}

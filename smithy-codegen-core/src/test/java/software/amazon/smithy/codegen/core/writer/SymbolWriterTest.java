/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.codegen.core.writer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolDependency;
import software.amazon.smithy.codegen.core.SymbolReference;

public class SymbolWriterTest {

    @Test
    public void managesDependencies() {
        MySimpleWriter writer = new MySimpleWriter("foo");
        SymbolDependency dep = SymbolDependency.builder()
                .packageName("foo")
                .version("123")
                .dependencyType("Dev")
                .build();
        writer.addDependency(dep);

        assertThat(writer.getDependencies(), contains(dep));
    }

    @Test
    public void writesDocumentationWithSanitation() {
        MySimpleWriter writer = new MySimpleWriter("foo");
        writer.writeDocs("Hi $dollar!");
        String result = writer.toString();

        assertThat(result, equalTo("Before\nHi $dollar!!\nAfter\n"));
    }

    @Test
    public void addsUseImportsWithReferences() {
        MySimpleWriter writer = new MySimpleWriter("foo");
        Symbol s = Symbol.builder()
                .declarationFile("foo.ts")
                .definitionFile("foo.ts")
                .name("Hello")
                .namespace("com/foo", "/")
                .build();
        SymbolReference reference = SymbolReference.builder()
                .symbol(s)
                .alias("X")
                .options(SymbolReference.ContextOption.USE)
                .build();
        writer.addUseImports(reference);

        assertThat(writer.getImportContainer().imports, hasKey("X"));
        assertThat(writer.getImportContainer().imports.get("X"), equalTo("com/foo/Hello"));
    }

    @Test
    public void omitsUseImportsWithReferencesIfSameNamespace() {
        MySimpleWriter writer = new MySimpleWriter("foo");
        Symbol s = Symbol.builder()
                .declarationFile("foo.ts")
                .definitionFile("foo.ts")
                .name("Hello")
                .namespace("foo", "/")
                .build();
        SymbolReference reference = SymbolReference.builder()
                .symbol(s)
                .alias("X")
                .options(SymbolReference.ContextOption.USE)
                .build();
        writer.addUseImports(reference);

        assertThat(writer.getImportContainer().imports, not(hasKey("X")));
    }

    @Test
    public void importsUseReferencesFromSymbols() {
        MySimpleWriter writer = new MySimpleWriter("foo");
        Symbol string = Symbol.builder()
                .definitionFile("java/lang/String.java")
                .name("String")
                .namespace("java.lang", ".")
                .build();
        SymbolReference reference = SymbolReference.builder()
                .symbol(string)
                .alias("MyString")
                .build();
        Symbol someList = Symbol.builder()
                .definitionFile("java/util/List.java")
                .name("List")
                .namespace("java.util", ".")
                .addReference(reference)
                .build();
        writer.addUseImports(someList);

        assertThat(writer.getImportContainer().imports, hasKey("List"));
        assertThat(writer.getImportContainer().imports, hasKey("MyString"));
        assertThat(writer.getImportContainer().imports.get("MyString"), equalTo("java.lang.String"));
    }

    @Test
    public void formatsSymbolsWithNoNamespaceRelativization() {
        MySimpleWriter writer = new MySimpleWriter("java.lang");
        // This symbol should *not* be relativized.
        Symbol string = Symbol.builder().name("String").namespace("java.lang", ".").build();
        writer.write("$T", string);

        assertThat(writer.toString(), equalTo("java.lang.String\n"));
    }

    @Test
    public void formatsSymbolsWithNamespaceRelativization() {
        MySimpleWriter writer = new MySimpleWriter("java.lang");
        // normally the constructor would call this automatically, but this is a test case!
        writer.setRelativizeSymbols("java.lang");
        // This symbol should be relativized.
        Symbol string = Symbol.builder().name("String").namespace("java.lang", ".").build();
        writer.write("$T", string);

        assertThat(writer.toString(), equalTo("String\n"));
    }

    @Test
    public void formatsSymbolReferences() {
        MySimpleWriter writer = new MySimpleWriter("com.foo");
        Symbol string = Symbol.builder().name("String").namespace("example.foo", ".").build();
        SymbolReference reference = SymbolReference.builder()
                .alias("Str")
                .symbol(string)
                .build();
        writer.write("$T", reference);

        assertThat(writer.toString(), equalTo("Str\n"));

        // The reference automatically adds imports.
        assertThat(writer.getImportContainer().imports, hasEntry("Str", string.toString()));
    }
}

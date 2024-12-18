/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SymbolTest {
    @Test
    public void relativizesSymbol() {
        String ns = "com.foo";
        Symbol symbol = Symbol.builder()
                .name("Baz")
                .namespace(ns, "::")
                .build();

        assertThat(symbol.relativize(ns), equalTo("Baz"));
        assertThat(symbol.relativize("com.bam"), equalTo("com.foo::Baz"));
    }

    @Test
    public void getsPropertiesFromClass() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .putProperty("baz", "bar")
                .putProperty("bam", 100)
                .build();

        assertThat(symbol.expectProperty("baz", String.class), equalTo("bar"));
        assertThat(symbol.expectProperty("bam", Integer.class), equalTo(100));
    }

    @Test
    public void getsTypedProperties() {
        Property<String> stringProperty = Property.named("string");
        Property<Integer> integerProperty = Property.named("int");

        Symbol symbol = Symbol.builder()
                .name("foo")
                .putProperty(stringProperty, "foo")
                .putProperty(integerProperty, 100)
                .build();

        assertThat(symbol.expectProperty(stringProperty), equalTo("foo"));
        assertThat(symbol.expectProperty(integerProperty), equalTo(100));
    }

    @Test
    public void throwsIfExpectedPropertyIsNotPresent() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Symbol symbol = Symbol.builder().name("foo").build();

            symbol.expectProperty("baz");
        });
    }

    @Test
    public void throwsIfExpectedTypedPropertyIsNotPresent() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Property<String> prop = Property.named("prop");
            Symbol symbol = Symbol.builder().name("foo").build();

            symbol.expectProperty(prop);
        });
    }

    @Test
    public void throwsIfExpectedPropertyIsNotOfSameType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Symbol symbol = Symbol.builder()
                    .name("foo")
                    .putProperty("bam", 100)
                    .build();

            symbol.expectProperty("bam", String.class);
        });
    }

    @Test
    public void returnsDefinitionIfDeclarationPresent() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .declarationFile("/foo/bar.baz")
                .build();

        assertThat(symbol.getDefinitionFile(), equalTo("/foo/bar.baz"));
    }

    @Test
    public void returnsDeclarationIfDefinitionPresent() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .declarationFile("/foo/bar.baz")
                .build();

        assertThat(symbol.getDeclarationFile(), equalTo("/foo/bar.baz"));
    }

    @Test
    public void returnsAppropriateDefinitionAndDeclarationFiles() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .definitionFile("/foo/bar.baz")
                .declarationFile("/foo/bar.h")
                .build();

        assertThat(symbol.getDefinitionFile(), equalTo("/foo/bar.baz"));
        assertThat(symbol.getDeclarationFile(), equalTo("/foo/bar.h"));
    }

    @Test
    public void canAddDependencies() {
        SymbolDependency a = SymbolDependency.builder().packageName("a1").version("a2").build();
        SymbolDependency b = SymbolDependency.builder().packageName("b1").version("b2").build();

        Symbol symbol = Symbol.builder()
                .name("foo")
                .addDependency("a1", "a2")
                .addDependency(SymbolDependency.builder().packageName("b1").version("b2").build())
                .build();

        assertThat(symbol.getDependencies(), containsInAnyOrder(a, b));
        assertThat(symbol.toBuilder().build(), equalTo(symbol));
    }

    @Test
    public void convertsToAliasedSymbolReference() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .namespace("bar", ".")
                .build();
        SymbolReference reference = symbol.toReference("__foo", SymbolReference.ContextOption.DECLARE);

        assertThat(reference.getSymbol(), is(symbol));
        assertThat(reference.getAlias(), equalTo("__foo"));
        assertThat(reference.getOptions(), contains(SymbolReference.ContextOption.DECLARE));
    }

    @Test
    public void convertsToAliasedSymbol() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .namespace("bar", ".")
                .build();

        Symbol symbolRef = symbol.toReferencedSymbol("__foo");

        SymbolReference ref = SymbolReference.builder()
                .alias("__foo")
                .symbol(symbol)
                .options(SymbolReference.ContextOption.USE)
                .build();

        assertThat(symbolRef.getName(), equalTo("__foo"));
        assertThat(symbolRef.getNamespace(), equalTo(""));
        assertThat(symbolRef.getReferences(), contains(ref));
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;

public class ReservedWordSymbolProviderTest {
    @Test
    public void escapesReservedFilenames() {
        Shape s1 = StringShape.builder().id("foo.bar#Baz").build();
        Shape s2 = StringShape.builder().id("foo.bar#Bam").build();

        ReservedWords reservedWords = new ReservedWordsBuilder().put("/foo/bar/bam", "/rewritten").build();
        MockProvider delegate = new MockProvider();
        SymbolProvider provider = ReservedWordSymbolProvider.builder()
                .symbolProvider(delegate)
                .filenameReservedWords(reservedWords)
                .build();
        delegate.mock = Symbol.builder()
                .name("foo")
                .definitionFile("/foo/bar/bam")
                .declarationFile("/foo/bar/bam")
                .build();

        assertThat(provider.toSymbol(s1).getDeclarationFile(), equalTo("/rewritten"));
        assertThat(provider.toSymbol(s2).getDefinitionFile(), equalTo("/rewritten"));
    }

    @Test
    public void escapesReservedNamespaces() {
        Shape s1 = StringShape.builder().id("foo.bar#Baz").build();
        Shape s2 = StringShape.builder().id("foo.baz#Bam").build();

        ReservedWords reservedWords = new ReservedWordsBuilder().put("foo.baz", "foo._baz").build();
        MockProvider delegate = new MockProvider();
        SymbolProvider provider = ReservedWordSymbolProvider.builder()
                .symbolProvider(delegate)
                .namespaceReservedWords(reservedWords)
                .build();

        delegate.mock = Symbol.builder().namespace("foo.bar", ".").name("Baz").build();
        assertThat(provider.toSymbol(s1).getNamespace(), equalTo("foo.bar"));

        delegate.mock = Symbol.builder().namespace("foo.baz", ".").name("Bam").build();
        assertThat(provider.toSymbol(s2).getNamespace(), equalTo("foo._baz"));
    }

    @Test
    public void escapesReservedNames() {
        Shape s1 = StringShape.builder().id("foo.bar#Baz").build();
        Shape s2 = StringShape.builder().id("foo.baz#Bam").build();

        ReservedWords reservedWords = new ReservedWordsBuilder().put("Bam", "_Bam").build();
        MockProvider delegate = new MockProvider();
        SymbolProvider provider = ReservedWordSymbolProvider.builder()
                .symbolProvider(delegate)
                .nameReservedWords(reservedWords)
                .build();

        delegate.mock = Symbol.builder().namespace("foo.bar", ".").name("Baz").build();
        assertThat(provider.toSymbol(s1).getName(), equalTo("Baz"));

        delegate.mock = Symbol.builder().namespace("foo.baz", ".").name("Bam").build();
        assertThat(provider.toSymbol(s2).getName(), equalTo("_Bam"));
    }

    @Test
    public void escapesReservedMemberNames() {
        MemberShape s1 = MemberShape.builder().id("foo.bar#Baz$foo").target("foo.baz#T").build();
        MemberShape s2 = MemberShape.builder().id("foo.baz#Baz$baz").target("foo.baz#T").build();

        ReservedWords reservedWords = new ReservedWordsBuilder().put("baz", "_baz").build();
        SymbolProvider delegate = new MockProvider();
        SymbolProvider provider = ReservedWordSymbolProvider.builder()
                .symbolProvider(delegate)
                .memberReservedWords(reservedWords)
                .build();

        assertThat(provider.toMemberName(s1), equalTo("foo"));
        assertThat(provider.toMemberName(s2), equalTo("_baz"));
    }

    @Test
    public void escapesOnlyWhenPredicateReturnsTrue() {
        Shape stringShape = StringShape.builder().id("foo.baz#Bam").build();

        ReservedWords reservedWords = new ReservedWordsBuilder().put("Bam", "_Bam").build();
        MockProvider delegate = new MockProvider();
        SymbolProvider provider = ReservedWordSymbolProvider.builder()
                .symbolProvider(delegate)
                .nameReservedWords(reservedWords)
                .escapePredicate((shape, symbol) -> false)
                .build();

        delegate.mock = Symbol.builder().namespace("foo.baz", ".").name("Bam").build();
        assertThat(provider.toSymbol(stringShape).getName(), equalTo("Bam"));
    }

    @Test
    public void canCreateEscaper() {
        MemberShape s1 = MemberShape.builder().id("foo.bar#Baz$baz").target("foo.baz#T").build();

        ReservedWords reservedWords = new ReservedWordsBuilder().put("baz", "_baz").build();
        SymbolProvider delegate = new MockProvider();
        ReservedWordSymbolProvider.Escaper escaper = ReservedWordSymbolProvider.builder()
                .memberReservedWords(reservedWords)
                .buildEscaper();

        assertThat(escaper.escapeMemberName(delegate.toMemberName(s1)), equalTo("_baz"));
    }

    private static final class MockProvider implements SymbolProvider {
        public Symbol mock;

        @Override
        public Symbol toSymbol(Shape shape) {
            return mock;
        }
    }
}

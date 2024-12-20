/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.MapUtils;

public class SymbolReferenceTest {
    @Test
    public void addsDefaultOptions() {
        Symbol symbol = Symbol.builder().name("foo").build();
        SymbolReference ref = new SymbolReference(symbol);

        assertThat(ref.hasOption(SymbolReference.ContextOption.USE), is(true));
        assertThat(ref.hasOption(SymbolReference.ContextOption.DECLARE), is(true));
    }

    @Test
    public void propertiesAndOptionsAreUsedInEquals() {
        Symbol symbol = Symbol.builder().name("foo").build();
        SymbolReference ref1 = new SymbolReference(symbol);
        SymbolReference ref2 = new SymbolReference(
                symbol,
                MapUtils.of("foo", true),
                SymbolReference.ContextOption.USE);
        SymbolReference ref3 = new SymbolReference(symbol, SymbolReference.ContextOption.USE);

        assertThat(ref1, equalTo(ref1));
        assertThat(ref1, not(equalTo(ref2)));
        assertThat(ref1, not(equalTo(ref3)));
        assertThat(ref2, not(equalTo(ref3)));
    }

    @Test
    public void canAssignAlias() {
        Symbol symbol = Symbol.builder().name("foo").build();
        SymbolReference ref1 = SymbolReference.builder().symbol(symbol).alias("$foo").build();
        SymbolReference ref2 = new SymbolReference(symbol);

        assertThat(ref1, not(equalTo(ref2)));
        assertThat(ref1.getSymbol().getName(), equalTo("foo"));
        assertThat(ref1.getAlias(), equalTo("$foo"));
        assertThat(ref1.toString(), containsString("'$foo'"));

        assertThat(ref2.getSymbol().getName(), equalTo("foo"));
        assertThat(ref2.getAlias(), equalTo("foo"));
    }

    @Test
    public void convertsToBuilder() {
        Symbol symbol = Symbol.builder().name("foo").build();
        SymbolReference ref1 = SymbolReference.builder().symbol(symbol).alias("$foo").build();

        assertThat(ref1.toBuilder().build(), equalTo(ref1));
    }
}

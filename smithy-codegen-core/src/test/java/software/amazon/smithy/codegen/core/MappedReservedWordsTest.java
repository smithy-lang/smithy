/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class MappedReservedWordsTest {
    @Test
    public void mapsOverReservedWords() {
        ReservedWords reservedWords = MappedReservedWords.builder()
                .put("foo", "Foo")
                .put("baz", "Baz")
                .build();

        assertThat(reservedWords.isReserved("foo"), is(true));
        assertThat(reservedWords.isReserved("Foo"), is(false));
        assertThat(reservedWords.isReserved("baz"), is(true));
        assertThat(reservedWords.isReserved("Baz"), is(false));
        assertThat(reservedWords.isReserved("qux"), is(false));

        assertThat(reservedWords.escape("foo"), equalTo("Foo"));
        assertThat(reservedWords.escape("Foo"), equalTo("Foo"));
        assertThat(reservedWords.escape("baz"), equalTo("Baz"));
        assertThat(reservedWords.escape("Baz"), equalTo("Baz"));
        assertThat(reservedWords.escape("qux"), equalTo("qux"));
    }

    @Test
    public void mapsOverReservedWordsCaseInsensitively() {
        ReservedWords reservedWords = MappedReservedWords.builder()
                .putCaseInsensitive("foo", "Foo")
                .putCaseInsensitive("baz", "Baz")
                .build();

        assertThat(reservedWords.isReserved("foo"), is(true));
        assertThat(reservedWords.isReserved("Foo"), is(true));
        assertThat(reservedWords.isReserved("baz"), is(true));
        assertThat(reservedWords.isReserved("Baz"), is(true));
        assertThat(reservedWords.isReserved("qux"), is(false));

        assertThat(reservedWords.escape("foo"), equalTo("Foo"));
        assertThat(reservedWords.escape("Foo"), equalTo("Foo"));
        assertThat(reservedWords.escape("baz"), equalTo("Baz"));
        assertThat(reservedWords.escape("Baz"), equalTo("Baz"));
        assertThat(reservedWords.escape("qux"), equalTo("qux"));
    }
}

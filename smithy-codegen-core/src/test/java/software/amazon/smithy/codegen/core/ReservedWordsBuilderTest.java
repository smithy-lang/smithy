/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class ReservedWordsBuilderTest {
    @Test
    public void loadsWords() {
        ReservedWords words = new ReservedWordsBuilder()
                .loadWords(getClass().getResource("words.txt"))
                .build();

        basicWordTestTest(words, "_");
        assertThat(words.escape("Boolean"), equalTo("Boolean"));
    }

    private void basicWordTestTest(ReservedWords words, String prefix) {
        assertThat(words.escape("undefined"), equalTo(prefix + "undefined"));
        assertThat(words.escape("null"), equalTo(prefix + "null"));
        assertThat(words.escape("string"), equalTo(prefix + "string"));
        assertThat(words.escape("boolean"), equalTo(prefix + "boolean"));
        assertThat(words.escape("random"), equalTo("random"));
    }

    @Test
    public void loadsCaseInsensitiveWords() {
        ReservedWords words = new ReservedWordsBuilder()
                .loadCaseInsensitiveWords(getClass().getResource("words.txt"))
                .build();

        basicWordTestTest(words, "_");
        assertThat(words.escape("Boolean"), equalTo("_Boolean"));
    }

    @Test
    public void loadsWordsWithCustomEscaper() {
        ReservedWords words = new ReservedWordsBuilder()
                .loadWords(getClass().getResource("words.txt"), word -> "$" + word)
                .build();

        basicWordTestTest(words, "$");
        assertThat(words.escape("Boolean"), equalTo("Boolean"));
    }

    @Test
    public void loadsCaseInsensitiveWordsWithCustomEscaper() {
        ReservedWords words = new ReservedWordsBuilder()
                .loadCaseInsensitiveWords(getClass().getResource("words.txt"), word -> "$" + word)
                .build();

        basicWordTestTest(words, "$");
        assertThat(words.escape("Boolean"), equalTo("$Boolean"));
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import java.nio.CharBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringTableTest {
    @Test
    public void cachesAndReturnsStringValues() {
        StringTable table = new StringTable();

        CharBuffer originalFoo = CharBuffer.wrap(new char[] {'f', 'o', 'o'});
        String internedFoo = table.apply(originalFoo);

        assertThat(table.apply(originalFoo), equalTo(internedFoo));
        assertThat(table.apply(originalFoo), sameInstance(internedFoo));
    }

    @Test
    public void overwritePreviousValuesWhenFull() {
        StringTable table = new StringTable(1); // 2 entries

        CharBuffer originalFoo = CharBuffer.wrap(new char[] {'f', 'o', 'o'});
        CharBuffer originalFoo1 = CharBuffer.wrap(new char[] {'f', 'o', 'o', '1'});
        CharBuffer originalFoo2 = CharBuffer.wrap(new char[] {'f', 'o', 'o', '2'});

        String internedFoo = table.apply(originalFoo);

        assertThat(internedFoo, sameInstance(table.apply(originalFoo)));
        assertThat(internedFoo, equalTo(originalFoo.toString()));
        assertThat(internedFoo, not(sameInstance(originalFoo.toString())));

        String internedFoo1 = table.apply(originalFoo1);
        String internedFoo2 = table.apply(originalFoo2);

        assertThat(internedFoo1, sameInstance(table.apply(originalFoo1)));
        assertThat(internedFoo1, equalTo(originalFoo1.toString()));
        assertThat(internedFoo1, not(sameInstance(originalFoo1.toString())));

        assertThat(internedFoo2, sameInstance(table.apply(originalFoo2)));
        assertThat(internedFoo2, equalTo(originalFoo2.toString()));
        assertThat(internedFoo2, not(sameInstance(originalFoo2.toString())));

        // The cache is now full. Overwrite an entry and store 'foo', causing the previously interned instance to
        // differ from the newly computed instance.
        String nextInternedFoo = table.apply(originalFoo);
        assertThat(nextInternedFoo, equalTo(originalFoo.toString()));
        assertThat(nextInternedFoo, not(sameInstance(internedFoo)));
    }

    @Test
    public void doesNotCreateTooBigOfCache() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new StringTable(17));
    }

    @Test
    public void doesNotCreateTooSmallOfCache() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new StringTable(0));
    }
}

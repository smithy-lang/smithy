/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SmfSharedSymbolsTest {

    @Test
    public void tableIsNotEmpty() {
        assertTrue(SmfSharedSymbols.size() > 0);
    }

    @Test
    public void noDuplicateEntries() {
        Set<String> seen = new HashSet<>();
        for (String symbol : SmfSharedSymbols.SYMBOLS) {
            assertTrue(seen.add(symbol), "Duplicate shared symbol: " + symbol);
        }
    }

    @Test
    public void allEntriesAreNonNull() {
        for (int i = 0; i < SmfSharedSymbols.SYMBOLS.length; i++) {
            assertNotNull(SmfSharedSymbols.SYMBOLS[i], "Null entry at index " + i);
        }
    }

    @Test
    public void allEntriesAreNonEmpty() {
        for (int i = 0; i < SmfSharedSymbols.SYMBOLS.length; i++) {
            assertTrue(!SmfSharedSymbols.SYMBOLS[i].isEmpty(),
                    "Empty string at index " + i);
        }
    }

    @Test
    public void reverseIndexIsConsistentWithForwardLookup() {
        for (int i = 0; i < SmfSharedSymbols.SYMBOLS.length; i++) {
            String symbol = SmfSharedSymbols.SYMBOLS[i];
            int id = i + 1; // 1-based
            assertEquals(id,
                    SmfSharedSymbols.getId(symbol),
                    "Reverse lookup mismatch for: " + symbol);
            assertEquals(symbol,
                    SmfSharedSymbols.getString(id),
                    "Forward lookup mismatch for ID: " + id);
        }
    }

    @Test
    public void getIdReturnsZeroForUnknownStrings() {
        assertEquals(0, SmfSharedSymbols.getId("not.in#Table"));
        assertEquals(0, SmfSharedSymbols.getId(""));
    }

    @Test
    public void getStringReturnsNullForOutOfRangeIds() {
        assertNull(SmfSharedSymbols.getString(0));
        assertNull(SmfSharedSymbols.getString(-1));
        assertNull(SmfSharedSymbols.getString(SmfSharedSymbols.size() + 1));
    }

    @Test
    public void allEntriesContainHashSeparator() {
        // All shared symbols should be valid shape IDs (namespace#name)
        for (String symbol : SmfSharedSymbols.SYMBOLS) {
            assertTrue(symbol.contains("#"),
                    "Shared symbol missing '#' separator: " + symbol);
        }
    }

    @Test
    public void reverseIndexSizeMatchesSymbolsLength() {
        assertEquals(SmfSharedSymbols.SYMBOLS.length,
                SmfSharedSymbols.REVERSE_INDEX.size());
    }

    @Test
    public void sizeMatchesArrayLength() {
        assertEquals(SmfSharedSymbols.SYMBOLS.length, SmfSharedSymbols.size());
    }
}

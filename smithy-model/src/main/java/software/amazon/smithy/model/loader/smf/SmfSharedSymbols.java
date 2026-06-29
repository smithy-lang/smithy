/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The SMF shared symbol table (version 1).
 *
 * <p>Contains prelude shape IDs that are compiled into every reader and writer.
 * These symbols are never transmitted in the file — they are referenced by
 * their position in this table (IDs 1 through SYMBOLS.length).
 *
 * <p>This table is append-only: future versions add entries at the end but
 * never reorder or remove existing entries.
 */
final class SmfSharedSymbols {

    static final int VERSION = 1;

    /**
     * Shared symbols indexed from 0. Symbol ID 1 corresponds to SYMBOLS[0],
     * symbol ID N corresponds to SYMBOLS[N-1].
     */
    static final String[] SYMBOLS = {
            "smithy.api#Unit",
            "smithy.api#Boolean",
            "smithy.api#Byte",
            "smithy.api#Short",
            "smithy.api#Integer",
            "smithy.api#Long",
            "smithy.api#Float",
            "smithy.api#Double",
            "smithy.api#BigDecimal",
            "smithy.api#BigInteger",
            "smithy.api#String",
            "smithy.api#Blob",
            "smithy.api#Timestamp",
            "smithy.api#Document",
            "smithy.api#PrimitiveBoolean",
            "smithy.api#PrimitiveByte",
            "smithy.api#PrimitiveShort",
            "smithy.api#PrimitiveInteger",
            "smithy.api#PrimitiveLong",
            "smithy.api#PrimitiveFloat",
            "smithy.api#PrimitiveDouble",
    };

    /**
     * Reverse lookup: string to symbol ID (1-based).
     */
    static final Map<String, Integer> REVERSE_INDEX;

    static {
        Map<String, Integer> map = new HashMap<>(SYMBOLS.length * 2);
        for (int i = 0; i < SYMBOLS.length; i++) {
            map.put(SYMBOLS[i], i + 1);
        }
        REVERSE_INDEX = Collections.unmodifiableMap(map);
    }

    private SmfSharedSymbols() {}

    /**
     * Returns the symbol ID for a string, or 0 if not in the shared table.
     */
    static int getId(String symbol) {
        return REVERSE_INDEX.getOrDefault(symbol, 0);
    }

    /**
     * Returns the string for a shared symbol ID (1-based), or null if out of range.
     */
    static String getString(int id) {
        if (id < 1 || id > SYMBOLS.length) {
            return null;
        }
        return SYMBOLS[id - 1];
    }

    /**
     * Returns the number of entries in the shared table.
     */
    static int size() {
        return SYMBOLS.length;
    }
}

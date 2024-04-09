/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import java.util.Arrays;
import java.util.function.Function;

/**
 * This is a simple, not thread-safe, caching string table that converts CharSequence to String objects.
 *
 * <p>The implementation uses an FNV-1a hash, and collisions simply overwrite the previously cached value.
 */
public final class StringTable implements Function<CharSequence, String> {

    private static final int FNV_OFFSET_BIAS = 0x811c9dc5;
    private static final int FNV_PRIME = 0x1000193;

    private final String[] table;
    private final int sizeBits;
    private final int size;
    private final int sizeMask;

    /**
     * Create a string table with 2048 entries.
     */
    public StringTable() {
        // Defaults to 2048 entries.
        this(11);
    }

    /**
     * Create a string table with a specific number of entries.
     *
     * @param sizeBits Size of the table based on bit shifting (e.g., 1 -> 2, 2 -> 4, ..., 10 -> 1024, 11 -> 2048).
     */
    public StringTable(int sizeBits) {
        if (sizeBits <= 0) {
            throw new IllegalArgumentException("Cache sizeBits must be >= 1");
        } else if (sizeBits >= 17) {
            throw new IllegalArgumentException("Refusing to create a cache with " + (1 << 17) + " entries");
        }

        this.sizeBits = sizeBits;
        this.size = (1 << sizeBits);
        this.sizeMask = size - 1;
        this.table = new String[size];
        Arrays.fill(table, "");
    }

    @Override
    public String apply(CharSequence chars) {
        int idx = localIdxFromHash(chars);
        String[] arr = table;
        String text = arr[idx];

        // On a cache hit, return the value if it matches. Otherwise, overwrite this value.
        if (textEquals(chars, text)) {
            return text;
        } else {
            String value = chars.toString();
            arr[idx] = value;
            return value;
        }
    }

    private int localIdxFromHash(CharSequence chars) {
        return getFnvHashCode(chars) & sizeMask;
    }

    private static int getFnvHashCode(CharSequence text) {
        int hashCode = FNV_OFFSET_BIAS;
        int end = text.length();

        for (int i = 0; i < end; i++) {
            hashCode = (hashCode ^ text.charAt(i)) * FNV_PRIME;
        }

        return hashCode;
    }

    private static boolean textEquals(CharSequence left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        for (int i = 0; i < left.length(); i++) {
            if (left.charAt(i) != right.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

/**
 * Matches a slice of a {@code char[]} against a fixed set of candidate keywords without allocating a string.
 *
 * <p>The candidates are supplied once at construction in a caller-defined order, and {@link #match} returns the index
 * of the matching candidate (or {@code -1}). Callers typically {@code switch} on the returned index. This lets a
 * parser dispatch on a keyword by comparing directly against its character buffer instead of allocating a substring
 * and comparing strings.
 *
 * <p>Matching works like a shallow trie: candidates are first bucketed by length (the cheapest discriminator,
 * precomputed once), and only candidates whose length equals the slice length are then compared character by
 * character. Keyword sets are typically small and largely length-distinct, so this eliminates most candidates before
 * any character comparison.
 */
public final class SliceMatcher {

    // Keywords stored as char[] so matching is a plain array-vs-array comparison, avoiding String.charAt's
    // Latin1/UTF16 coder dispatch on every character.
    private final char[][] keywords;
    private final int minLength;
    // byLength[len - minLength] holds the indices of every keyword whose length is len.
    private final int[][] byLength;

    /**
     * Creates a matcher over the given candidate keywords.
     *
     * <p>The index of each keyword in this array is what {@link #match} returns when that keyword matches. If a
     * keyword appears more than once, the lowest index wins.
     *
     * @param keywords Candidate keywords in the order callers will switch on.
     */
    public SliceMatcher(String... keywords) {
        this.keywords = new char[keywords.length][];
        for (int i = 0; i < keywords.length; i++) {
            this.keywords[i] = keywords[i].toCharArray();
        }

        if (keywords.length == 0) {
            this.minLength = 0;
            this.byLength = new int[0][];
            return;
        }

        int min = Integer.MAX_VALUE;
        int max = 0;
        for (char[] keyword : this.keywords) {
            min = Math.min(min, keyword.length);
            max = Math.max(max, keyword.length);
        }
        this.minLength = min;

        int[] counts = new int[max - min + 1];
        for (char[] keyword : this.keywords) {
            counts[keyword.length - min]++;
        }

        int[][] buckets = new int[counts.length][];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new int[counts[i]];
        }

        int[] fill = new int[counts.length];
        for (int i = 0; i < this.keywords.length; i++) {
            int bucket = this.keywords[i].length - min;
            buckets[bucket][fill[bucket]++] = i;
        }
        this.byLength = buckets;
    }

    /**
     * Matches the slice {@code chars[offset, offset + length)} against the candidates.
     *
     * @param chars Character buffer containing the slice.
     * @param offset Start of the slice (inclusive).
     * @param length Length of the slice.
     * @return The index of the matching candidate, or {@code -1} if none match.
     */
    public int match(char[] chars, int offset, int length) {
        int bucket = length - minLength;
        if (bucket < 0 || bucket >= byLength.length) {
            return -1;
        }

        outer:
        for (int candidate : byLength[bucket]) {
            char[] keyword = keywords[candidate];
            for (int i = 0; i < length; i++) {
                if (chars[offset + i] != keyword[i]) {
                    continue outer;
                }
            }
            return candidate;
        }

        return -1;
    }
}

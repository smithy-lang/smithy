/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import software.amazon.smithy.utils.StringUtils;

/**
 * Matches text based on word boundaries.
 *
 * <p>Note that this class is not thread safe due caching. If that ever needs
 * to change, we can reevaluate how to make {@link #test(String)} thread safe
 * (for example, by wrapping the cache in a synchronized map or by passing
 * in a function to customize how to normalize search text).
 */
final class WordBoundaryMatcher implements Predicate<String> {

    private final Set<String> words = new HashSet<>();

    // Use an LRU cache that stores up to 128 canonicalized search strings (e.g. don't parse "member" over and over).
    private final Map<String, String> searchCache = new LinkedHashMap<String, String>(128, 1.0f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return this.size() > 127;
        }
    };

    /**
     * Add a word boundary search terms to the matcher.
     *
     * @param terms Words separated by a single space.
     */
    public void addSearch(String terms) {
        if (StringUtils.isEmpty(terms)) {
            throw new IllegalArgumentException("Invalid empty search terms");
        }
        String wordPattern = parseWordPattern(terms);
        words.add(wordPattern);
        words.add(wordPattern.replace(" ", ""));
    }

    @Override
    public boolean test(String text) {
        if (text == null || text.isEmpty() || words.isEmpty()) {
            return false;
        }

        String haystack = searchCache.computeIfAbsent(text, WordBoundaryMatcher::splitWords);
        for (String needle : words) {
            if (testWordMatch(needle, haystack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the first term that the input text matched.
     * @param text the String within which to search for matches
     * @return the first match found
     */
    public Optional<String> getFirstMatch(String text) {
        if (text == null || text.isEmpty() || words.isEmpty()) {
            return Optional.empty();
        }

        String haystack = searchCache.computeIfAbsent(text, WordBoundaryMatcher::splitWords);
        for (String needle : words) {
            if (testWordMatch(needle, haystack)) {
                return Optional.of(needle);
            }
        }

        return Optional.empty();
    }

    private boolean testWordMatch(String needle, String haystack) {
        int position = haystack.indexOf(needle);
        int haystackLength = haystack.length();
        int needleLength = needle.length();
        if (position == -1) {
            return false;
        } else if (needleLength == haystackLength) {
            return true;
        } else if (position == 0) {
            return haystack.charAt(needleLength) == ' ';
        } else if (position == haystackLength - needleLength) {
            return haystack.charAt(position - 1) == ' ';
        } else {
            return haystack.charAt(position - 1) == ' ' && haystack.charAt(position + needleLength) == ' ';
        }
    }

    private static String parseWordPattern(String pattern) {
        boolean previousSpace = false;
        StringBuilder result = new StringBuilder(pattern.length() - 2);
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            result.append(Character.toLowerCase(c));
            if (c == ' ') {
                // Ensure that extraneous spaces aren't found at the beginning, end, or between words.
                if (i == 0 || i == pattern.length() - 1 || previousSpace) {
                    throw new IllegalArgumentException("Invalid spaces in word boundary search: " + pattern);
                } else {
                    previousSpace = true;
                }
            } else if (!Character.isLetterOrDigit(c)) {
                throw new IllegalArgumentException(
                        "Invalid non-alphanumeric characters in word boundary search:" + pattern);
            } else {
                previousSpace = false;
            }
        }

        return result.toString();
    }

    // Rather than return a list of words, this method canonicalizes words into a lowercase, space-delimited string.
    // This allows substring checks to be used to determine if a search is found in the delimited words.
    // Adapted from Apache Commons Lang3: https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/StringUtils.java#L7457
    private static String splitWords(String str) {
        if (str.isEmpty()) {
            return "";
        }

        final StringBuilder result = new StringBuilder();
        int tokenStart = 0;
        int currentType = Character.getType(str.charAt(tokenStart));

        for (int pos = tokenStart + 1; pos < str.length(); pos++) {
            final char c = str.charAt(pos);
            final int type = Character.getType(c);
            if (type == currentType) {
                continue;
            }
            if (type == Character.LOWERCASE_LETTER && currentType == Character.UPPERCASE_LETTER) {
                final int newTokenStart = pos - 1;
                if (newTokenStart != tokenStart) {
                    addLowerCaseStringToBuilder(result, str, tokenStart, newTokenStart - tokenStart);
                    result.append(' ');
                    tokenStart = newTokenStart;
                }
            } else {
                // Skip character groupings that are delimiters. We just want letters and numbers.
                if (Character.isLetterOrDigit(str.charAt(pos - 1))) {
                    addLowerCaseStringToBuilder(result, str, tokenStart, pos - tokenStart);
                    result.append(' ');
                }
                tokenStart = pos;
            }
            currentType = type;
        }

        if (Character.isLetterOrDigit(str.charAt(tokenStart))) {
            // Add the last segment if it's a letter or number.
            addLowerCaseStringToBuilder(result, str, tokenStart, str.length() - tokenStart);
        } else {
            // Since the last segment is ignored, remove the trailing space.
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    private static void addLowerCaseStringToBuilder(StringBuilder result, String str, int start, int count) {
        for (int i = start; i < start + count; i++) {
            result.append(Character.toLowerCase(str.charAt(i)));
        }
    }
}

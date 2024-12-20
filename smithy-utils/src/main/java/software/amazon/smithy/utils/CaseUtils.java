/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Provides support for camelCase, snake_case, and other kinds
 * of case conversions.
 */
public final class CaseUtils {

    private CaseUtils() {}

    /**
     * <p>Converts all words separated by "_" into PascalCase, that is each
     * word is made up of a titlecase character and then a series of
     * lowercase characters.</p>
     *
     * <p>PacalCase is just like CamelCase, except the first character is an
     * uppercase letter.</p>
     *
     * @param str  the String to be converted to PascalCase, may be null
     * @return camelCase of String, <code>null</code> if null String input
     */
    public static String snakeToPascalCase(String str) {
        return toCamelCase(str, true, '_');
    }

    /**
     * <p>Converts all the delimiter separated words in a String into camelCase,
     * that is each word is made up of a titlecase character and then a series of
     * lowercase characters.
     *
     * <p>The first character is always converted to lowercase.</p>
     *
     * @param str the String to be converted to camelCase, may be null
     * @return camelCase of String, <code>null</code> if null String input
     */
    public static String snakeToCamelCase(String str) {
        return toCamelCase(str, false, '_');
    }

    /**
     * <p>Converts all words separated by " ", "-", and "_" to CamelCase.
     *
     * <p>The first character is always converted to lowercase.</p>
     *
     * @param str the String to be converted to camelCase, may be null
     * @return camelCase of String, <code>null</code> if null String input
     */
    public static String toCamelCase(String str) {
        return toCamelCase(str, false, '_', '-', ' ');
    }

    /**
     * <p>Converts all words separated by " ", "-", and "_" to CamelCase.</p>
     *
     * <p>PacalCase is just like CamelCase, except the first character is an
     * uppercase letter.</p>
     *
     * @param str  the String to be converted to PascalCase, may be null
     * @return camelCase of String, <code>null</code> if null String input
     */
    public static String toPascalCase(String str) {
        return toCamelCase(str, true, '_', '-', ' ');
    }

    /**
     * <p>Converts all the delimiter separated words in a String into camelCase,
     * that is each word is made up of a titlecase character and then a series of
     * lowercase characters.</p>
     *
     * <p>The delimiters represent a set of characters understood to separate words.
     * The first non-delimiter character after a delimiter will be capitalized. The first String
     * character may or may not be capitalized and it's determined by the user input for capitalizeFirstLetter
     * variable.</p>
     *
     * <p>A <code>null</code> input String returns <code>null</code>.
     * Capitalization uses the Unicode title case, normally equivalent to
     * upper case and cannot perform locale-sensitive mappings.</p>
     *
     * <pre>
     * CaseUtils.toCamelCase(null, false)                                 = null
     * CaseUtils.toCamelCase("", false, *)                                = ""
     * CaseUtils.toCamelCase(*, false, null)                              = *
     * CaseUtils.toCamelCase(*, true, new char[0])                        = *
     * CaseUtils.toCamelCase("To.Camel.Case", false, new char[]{'.'})     = "toCamelCase"
     * CaseUtils.toCamelCase(" to @ Camel case", true, new char[]{'@'})   = "ToCamelCase"
     * CaseUtils.toCamelCase(" @to @ Camel case", false, new char[]{'@'}) = "toCamelCase"
     * </pre>
     *
     * @param str  the String to be converted to camelCase, may be null
     * @param capitalizeFirstLetter boolean that determines if the first character of first word should be title case.
     * @param delimiters  set of characters to determine capitalization, null and/or empty array means whitespace
     * @return camelCase of String, <code>null</code> if null String input
     * @see <a href="https://github.com/apache/commons-text/blob/c3b30de7352f8af85455d9b18778c9cd609ceb1d/src/main/java/org/apache/commons/text/CaseUtils.java">Source</a>
     */
    public static String toCamelCase(String str, final boolean capitalizeFirstLetter, final char... delimiters) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        str = str.toLowerCase();
        final int strLen = str.length();
        final int[] newCodePoints = new int[strLen];
        int outOffset = 0;
        final Set<Integer> delimiterSet = generateDelimiterSet(delimiters);
        boolean capitalizeNext = false;
        if (capitalizeFirstLetter) {
            capitalizeNext = true;
        }
        for (int index = 0; index < strLen;) {
            final int codePoint = str.codePointAt(index);

            if (delimiterSet.contains(codePoint)) {
                capitalizeNext = true;
                if (outOffset == 0) {
                    capitalizeNext = false;
                }
                index += Character.charCount(codePoint);
            } else if (capitalizeNext || outOffset == 0 && capitalizeFirstLetter) {
                final int titleCaseCodePoint = Character.toTitleCase(codePoint);
                newCodePoints[outOffset++] = titleCaseCodePoint;
                index += Character.charCount(titleCaseCodePoint);
                capitalizeNext = false;
            } else {
                newCodePoints[outOffset++] = codePoint;
                index += Character.charCount(codePoint);
            }
        }
        if (outOffset != 0) {
            return new String(newCodePoints, 0, outOffset);
        }
        return str;
    }

    /**
     * <p>Converts an array of delimiters to a hash set of code points. Code point of space(32) is added
     * as the default value. The generated hash set provides O(1) lookup time.</p>
     *
     * @param delimiters  set of characters to determine capitalization, null means whitespace
     * @return Set of Integer
     * @see <a href="https://github.com/apache/commons-text/blob/c3b30de7352f8af85455d9b18778c9cd609ceb1d/src/main/java/org/apache/commons/text/CaseUtils.java">Source</a>
     */
    private static Set<Integer> generateDelimiterSet(final char[] delimiters) {
        final Set<Integer> delimiterHashSet = new HashSet<>();
        delimiterHashSet.add(Character.codePointAt(new char[] {' '}, 0));
        if (delimiters == null || delimiters.length == 0) {
            return delimiterHashSet;
        }

        for (int index = 0; index < delimiters.length; index++) {
            delimiterHashSet.add(Character.codePointAt(delimiters, index));
        }
        return delimiterHashSet;
    }

    /**
     * Convert a given word to snake_case with all lowercase letters.
     *
     * <p>This method was based on Elephant Bird's underscore method.
     * "-", " ", "\n", "\t", "\r" are replaced with "_".
     *
     * <p>Note: this method does not sanitize the string for use as a snake_case
     * variable in any specific programming language.
     *
     * @param word The word to convert.
     * @return The underscored version of the word
     * @see <a href="https://github.com/twitter/elephant-bird/blob/master/core/src/main/java/com/twitter/elephantbird/util/Strings.java">Elephant bird</a>
     */
    public static String toSnakeCase(String word) {
        if (StringUtils.isEmpty(word)) {
            return word;
        }

        String firstPattern = "([A-Z]+)([A-Z][a-z])";
        String secondPattern = "([a-z\\d])([A-Z])";
        String replacementPattern = "$1_$2";
        // Replace capital letter with _ plus lowercase letter.
        word = word.replaceAll(firstPattern, replacementPattern);
        word = word.replaceAll(secondPattern, replacementPattern);
        word = word.replaceAll("(\\s|-)", "_");
        // Begin modification
        word = word.toLowerCase(Locale.US);
        // End modification
        return word;
    }
}

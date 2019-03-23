/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public final class StringUtils {
    /**
     * <p>Wraps a single line of text, identifying words by <code>' '</code>.</p>
     *
     * <p>New lines will be separated by the system property line separator.
     * Very long words, such as URLs will <i>not</i> be wrapped.</p>
     *
     * <p>Leading spaces on a new line are stripped.
     * Trailing spaces are not stripped.</p>
     *
     * <table border="1">
     *  <caption>Examples</caption>
     *  <tr>
     *   <th>input</th>
     *   <th>wrapLength</th>
     *   <th>result</th>
     *  </tr>
     *  <tr>
     *   <td>null</td>
     *   <td>*</td>
     *   <td>null</td>
     *  </tr>
     *  <tr>
     *   <td>""</td>
     *   <td>*</td>
     *   <td>""</td>
     *  </tr>
     *  <tr>
     *   <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
     *   <td>20</td>
     *   <td>"Here is one line of\ntext that is going\nto be wrapped after\n20 columns."</td>
     *  </tr>
     *  <tr>
     *   <td>"Click here to jump to the commons website - http://commons.apache.org"</td>
     *   <td>20</td>
     *   <td>"Click here to jump\nto the commons\nwebsite -\nhttp://commons.apache.org"</td>
     *  </tr>
     *  <tr>
     *   <td>"Click here, http://commons.apache.org, to jump to the commons website"</td>
     *   <td>20</td>
     *   <td>"Click here,\nhttp://commons.apache.org,\nto jump to the\ncommons website"</td>
     *  </tr>
     * </table>
     *
     * (assuming that '\n' is the systems line separator)
     *
     * @param str  the String to be word wrapped, may be null
     * @param wrapLength  the column to wrap the words at, less than 1 is treated as 1
     * @return a line with newlines inserted, <code>null</code> if null input
     * @see <a href="https://github.com/apache/commons-text/blob/f0ae79e46e3923562168df9c03023587eafc4d69/src/main/java/org/apache/commons/text/WordUtils.java#L105">Source</a>
     */
    public static String wrap(final String str, final int wrapLength) {
        return wrap(str, wrapLength, null, false, " ");
    }

    // https://github.com/apache/commons-text/blob/f0ae79e46e3923562168df9c03023587eafc4d69/src/main/java/org/apache/commons/text/WordUtils.java#L283
    private static String wrap(final String str,
            int wrapLength,
            String newLineStr,
            final boolean wrapLongWords,
            String wrapOn
    ) {
        if (str == null) {
            return null;
        }
        if (newLineStr == null) {
            newLineStr = System.lineSeparator();
        }
        if (wrapLength < 1) {
            wrapLength = 1;
        }
        if (wrapOn.isBlank()) {
            wrapOn = " ";
        }
        final Pattern patternToWrapOn = Pattern.compile(wrapOn);
        final int inputLineLength = str.length();
        int offset = 0;
        final StringBuilder wrappedLine = new StringBuilder(inputLineLength + 32);

        while (offset < inputLineLength) {
            int spaceToWrapAt = -1;
            Matcher matcher = patternToWrapOn.matcher(str.substring(
                    offset, Math.min((int) Math.min(Integer.MAX_VALUE, offset + wrapLength + 1L), inputLineLength)));
            if (matcher.find()) {
                if (matcher.start() == 0) {
                    offset += matcher.end();
                    continue;
                }
                spaceToWrapAt = matcher.start() + offset;
            }

            // only last line without leading spaces is left
            if (inputLineLength - offset <= wrapLength) {
                break;
            }

            while (matcher.find()) {
                spaceToWrapAt = matcher.start() + offset;
            }

            if (spaceToWrapAt >= offset) {
                // normal case
                wrappedLine.append(str, offset, spaceToWrapAt);
                wrappedLine.append(newLineStr);
                offset = spaceToWrapAt + 1;

            } else {
                // really long word or URL
                if (wrapLongWords) {
                    // wrap really long word one line at a time
                    wrappedLine.append(str, offset, wrapLength + offset);
                    wrappedLine.append(newLineStr);
                    offset += wrapLength;
                } else {
                    // do not wrap really long word, just extend beyond limit
                    matcher = patternToWrapOn.matcher(str.substring(offset + wrapLength));
                    if (matcher.find()) {
                        spaceToWrapAt = matcher.start() + offset + wrapLength;
                    }

                    if (spaceToWrapAt >= 0) {
                        wrappedLine.append(str, offset, spaceToWrapAt);
                        wrappedLine.append(newLineStr);
                        offset = spaceToWrapAt + 1;
                    } else {
                        wrappedLine.append(str, offset, str.length());
                        offset = inputLineLength;
                    }
                }
            }
        }

        // Whatever is left in line is short enough to just pass through
        wrappedLine.append(str, offset, str.length());

        return wrappedLine.toString();
    }

    /**
     * <p>Converts all the delimiter separated words in a String into PascalCase,
     * that is each word is made up of a titlecase character and then a series of
     * lowercase characters.</p>
     *
     * <p>PacalCase is just like CamelCase, except the first character is an
     * uppercase letter.
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

    // https://github.com/apache/commons-text/blob/f0ae79e46e3923562168df9c03023587eafc4d69/src/main/java/org/apache/commons/text/CaseUtils.java#L76
    private static String toCamelCase(String str, final boolean capitalizeFirstLetter, final char... delimiters) {
        // Begin modification
        if (str == null || str.isEmpty()) {
            return str;
        }
        str = str.toLowerCase(Locale.US);
        // End modification
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

    // https://github.com/apache/commons-text/blob/f0ae79e46e3923562168df9c03023587eafc4d69/src/main/java/org/apache/commons/text/WordUtils.java#L887
    private static Set<Integer> generateDelimiterSet(final char[] delimiters) {
        final Set<Integer> delimiterHashSet = new HashSet<>();
        delimiterHashSet.add(Character.codePointAt(new char[]{' '}, 0));
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
     * @see <a href="https://github.com/twitter/elephant-bird/blob/master/LICENSE">Elephant bird license</a>
     */
    public static String toSnakeCase(String word) {
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

    /**
     * <p>Capitalizes a String changing the first character to title case as
     * per {@link Character#toTitleCase(int)}. No other characters are changed.</p>
     *
     * <pre>
     * StringUtils.capitalize(null)  = null
     * StringUtils.capitalize("")    = ""
     * StringUtils.capitalize("cat") = "Cat"
     * StringUtils.capitalize("cAt") = "CAt"
     * StringUtils.capitalize("'cat'") = "'cat'"
     * </pre>
     *
     * @param str the String to capitalize, may be null
     * @return the capitalized String, {@code null} if null String input
     * @see <a href="https://github.com/apache/commons-lang/blob/c4d0dbcb56b8980b1b3b7c85d00ad6540788c08e/src/main/java/org/apache/commons/lang3/StringUtils.java#L6803">Source</a>
     * @see #uncapitalize(String)
     */
    public static String capitalize(final String str) {
        if (str == null) {
            return null;
        }

        int strLen = str.length();
        if (strLen == 0) {
            return str;
        }

        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toTitleCase(firstCodepoint);
        if (firstCodepoint == newCodePoint) {
            // already capitalized
            return str;
        }

        final int[] newCodePoints = new int[strLen]; // cannot be longer than the char array
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint; // copy the first codepoint
        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen;) {
            final int codepoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codepoint; // copy the remaining ones
            inOffset += Character.charCount(codepoint);
        }
        return new String(newCodePoints, 0, outOffset);
    }

    /**
     * <p>Uncapitalizes a String, changing the first character to lower case as
     * per {@link Character#toLowerCase(int)}. No other characters are changed.</p>
     *
     * <pre>
     * StringUtils.uncapitalize(null)  = null
     * StringUtils.uncapitalize("")    = ""
     * StringUtils.uncapitalize("cat") = "cat"
     * StringUtils.uncapitalize("Cat") = "cat"
     * StringUtils.uncapitalize("CAT") = "cAT"
     * </pre>
     *
     * @param str the String to uncapitalize, may be null
     * @return the uncapitalized String, {@code null} if null String input
     * @see <a href="https://github.com/apache/commons-lang/blob/c4d0dbcb56b8980b1b3b7c85d00ad6540788c08e/src/main/java/org/apache/commons/lang3/StringUtils.java#L6848">Source</a>
     */
    public static String uncapitalize(final String str) {
        if (str == null) {
            return str;
        }
        int strLen = str.length();
        if (strLen == 0) {
            return str;
        }

        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toLowerCase(firstCodepoint);
        if (firstCodepoint == newCodePoint) {
            // already capitalized
            return str;
        }

        final int[] newCodePoints = new int[strLen]; // cannot be longer than the char array
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint; // copy the first codepoint
        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen;) {
            final int codepoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codepoint; // copy the remaining ones
            inOffset += Character.charCount(codepoint);
        }
        return new String(newCodePoints, 0, outOffset);
    }

    /**
     * Escapes a given value to convert double quotes (") to (\").
     *
     * <p>Any backslash (\) is converted to double blackslashes (\\).
     *
     * @param value Value to escape.
     * @return Returns the escaped value.
     */
    public static String escapeDoubleQuote(String value) {
        return escapeDelimited(value, "\"");
    }

    /**
     * Escapes a given value to convert single quotes (') to (\').
     *
     * <p>Any backslash (\) is converted to double blackslashes (\\).
     *
     * @param value Value to escape.
     * @return Returns the escaped value.
     */
    public static String escapeSingleQuote(String value) {
        return escapeDelimited(value, "'");
    }

    private static String escapeDelimited(String value, String delimiter) {
        value = value.replace("\\", "\\\\");
        value = value.replace(delimiter, "\\" + delimiter);
        return value;
    }
}

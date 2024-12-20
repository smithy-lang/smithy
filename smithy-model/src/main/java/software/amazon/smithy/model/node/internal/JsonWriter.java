/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node.internal;

import java.io.IOException;
import java.io.Writer;

class JsonWriter {

    private static final int CONTROL_CHARACTERS_END = 0x001f;

    private static final char[] QUOT_CHARS = {'\\', '"'};
    private static final char[] BS_CHARS = {'\\', '\\'};
    private static final char[] LF_CHARS = {'\\', 'n'};
    private static final char[] CR_CHARS = {'\\', 'r'};
    private static final char[] TAB_CHARS = {'\\', 't'};
    // In JavaScript, U+2028 and U+2029 characters count as line endings and must be encoded.
    // http://stackoverflow.com/questions/2965293/javascript-parse-error-on-u2028-unicode-character
    private static final char[] UNICODE_2028_CHARS = {'\\', 'u', '2', '0', '2', '8'};
    private static final char[] UNICODE_2029_CHARS = {'\\', 'u', '2', '0', '2', '9'};
    private static final char[] HEX_DIGITS = {'0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'a',
            'b',
            'c',
            'd',
            'e',
            'f'};

    final Writer writer;

    JsonWriter(Writer writer) {
        this.writer = writer;
    }

    private static char[] getReplacementChars(char ch) {
        if (ch > '\\') {
            if (ch < '\u2028' || ch > '\u2029') {
                // The lower range contains 'a' .. 'z'. Only 2 checks required.
                return null;
            }
            return ch == '\u2028' ? UNICODE_2028_CHARS : UNICODE_2029_CHARS;
        }
        if (ch == '\\') {
            return BS_CHARS;
        }
        if (ch > '"') {
            // This range contains '0' .. '9' and 'A' .. 'Z'. Need 3 checks to get here.
            return null;
        }
        if (ch == '"') {
            return QUOT_CHARS;
        }
        if (ch > CONTROL_CHARACTERS_END) {
            return null;
        }
        if (ch == '\n') {
            return LF_CHARS;
        }
        if (ch == '\r') {
            return CR_CHARS;
        }
        if (ch == '\t') {
            return TAB_CHARS;
        }
        return new char[] {'\\', 'u', '0', '0', HEX_DIGITS[ch >> 4 & 0x000f], HEX_DIGITS[ch & 0x000f]};
    }

    void writeLiteral(String value) throws IOException {
        writer.write(value);
    }

    void writeNumber(String string) throws IOException {
        writer.write(string);
    }

    void writeString(String string) throws IOException {
        writer.write('"');
        writeJsonString(string);
        writer.write('"');
    }

    void writeArrayOpen() throws IOException {
        writer.write('[');
    }

    void writeArrayClose() throws IOException {
        writer.write(']');
    }

    void writeArraySeparator() throws IOException {
        writer.write(',');
    }

    void writeObjectOpen() throws IOException {
        writer.write('{');
    }

    void writeObjectClose() throws IOException {
        writer.write('}');
    }

    void writeMemberName(String name) throws IOException {
        writer.write('"');
        writeJsonString(name);
        writer.write('"');
    }

    void writeMemberSeparator() throws IOException {
        writer.write(':');
    }

    void writeObjectSeparator() throws IOException {
        writer.write(',');
    }

    void writeJsonString(String string) throws IOException {
        int length = string.length();
        int start = 0;
        for (int index = 0; index < length; index++) {
            char[] replacement = getReplacementChars(string.charAt(index));
            if (replacement != null) {
                writer.write(string, start, index - start);
                writer.write(replacement);
                start = index + 1;
            }
        }
        writer.write(string, start, length - start);
    }
}

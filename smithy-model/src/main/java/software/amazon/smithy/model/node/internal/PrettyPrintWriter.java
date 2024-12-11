/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node.internal;

import java.io.IOException;
import java.io.Writer;

/**
 * Enables human readable JSON output by inserting whitespace between values.after commas and
 * colons.
 *
 * Based on https://github.com/ralfstx/minimal-json/blob/master/com.eclipsesource.json/src/main/java/com/eclipsesource/json/PrettyPrint.java#L84
 */
final class PrettyPrintWriter extends JsonWriter {

    private final String indentString;
    private int indent;

    PrettyPrintWriter(Writer writer, String indentString) {
        super(writer);
        this.indentString = indentString;
    }

    @Override
    void writeArrayOpen() throws IOException {
        indent++;
        writer.write('[');
        writeNewLine();
    }

    @Override
    void writeArrayClose() throws IOException {
        indent--;
        writeNewLine();
        writer.write(']');
    }

    @Override
    void writeArraySeparator() throws IOException {
        writer.write(',');
        if (!writeNewLine()) {
            writer.write(' ');
        }
    }

    @Override
    void writeObjectOpen() throws IOException {
        indent++;
        writer.write('{');
        writeNewLine();
    }

    @Override
    void writeObjectClose() throws IOException {
        indent--;
        writeNewLine();
        writer.write('}');
    }

    @Override
    void writeMemberSeparator() throws IOException {
        writer.write(':');
        writer.write(' ');
    }

    @Override
    void writeObjectSeparator() throws IOException {
        writer.write(',');
        if (!writeNewLine()) {
            writer.write(' ');
        }
    }

    private boolean writeNewLine() throws IOException {
        if (indentString == null) {
            return false;
        }
        writer.write(System.lineSeparator());
        for (int i = 0; i < indent; i++) {
            writer.write(indentString);
        }
        return true;
    }
}

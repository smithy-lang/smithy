/*
 * Copyright (c) 2013, 2016 EclipseSource.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

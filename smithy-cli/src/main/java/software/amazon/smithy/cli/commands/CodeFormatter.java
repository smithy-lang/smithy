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

package software.amazon.smithy.cli.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Iterator;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.model.loader.sourcecontext.SourceContextLoader;

/**
 * Formats source {@link SourceContextLoader.Line} values and writes them to an {@link Appendable}.
 */
final class CodeFormatter {

    private final Appendable writer;
    private final ColorFormatter colors;
    private final int maxWidth;

    CodeFormatter(Appendable writer, ColorFormatter colors, int maxWidth) {
        this.writer = writer;
        this.colors = colors;
        this.maxWidth = maxWidth;
    }

    void writeCode(int cursorLine, int cursorColumn, Collection<SourceContextLoader.Line> lines) {
        if (lines.isEmpty()) {
            return;
        }

        // Determine the string length of the biggest number to pad the line gutter correctly.
        int numberLength = findLongestNumber(lines);

        try {
            Iterator<SourceContextLoader.Line> lineIterator = lines.iterator();
            int lastLine = -1;

            while (lineIterator.hasNext()) {
                SourceContextLoader.Line line = lineIterator.next();

                if (line.getLineNumber() != lastLine + 1 && lastLine != -1) {
                    writeColumnAndContent(numberLength, -1, "");
                }

                writeColumnAndContent(numberLength, line.getLineNumber(), line.getContent());

                if (line.getLineNumber() == cursorLine) {
                    writePointer(numberLength, cursorColumn);
                }

                lastLine = line.getLineNumber();
            }

            writer.append(System.lineSeparator());
        } catch (IOException e) {
            throw new UncheckedIOException("Error write source code: " + e.getMessage(), e);
        }
    }

    private void writeColumnAndContent(int numberLength, int lineNumber, CharSequence content) throws IOException {
        if (lineNumber == -1) {
            colors.style(writer, w -> {
                try {
                    for (int i = 0; i < numberLength; i++) {
                        w.append("·");
                    }
                    w.append("|");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, Style.BRIGHT_BLACK);
        } else {
            colors.style(writer, w -> {
                try {
                    String lineString = String.valueOf(lineNumber);
                    int thisLineLength = lineString.length();
                    writer.append(lineString);
                    // Write the appropriate amount of padding.
                    for (int i = 0; i < numberLength - thisLineLength; i++) {
                        writer.append(' ');
                    }
                    colors.style(writer, "| ", Style.BRIGHT_BLACK);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, Style.BRIGHT_BLACK);
        }

        if (content.length() > 0) {
            writeStringWithMaxWidth(content, numberLength);
        }

        writer.append(System.lineSeparator());
    }

    private void writePointer(int numberLength, int cursorColumn) {
        colors.style(writer, w -> {
            try {
                for (int j = 0; j < numberLength; j++) {
                    writer.append(' ');
                }
                writer.append("|");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, Style.BRIGHT_BLACK);

        try {
            for (int j = 0; j < cursorColumn; j++) {
                writer.append(' ');
            }
            colors.style(writer, "^", Style.RED);
            writer.append(System.lineSeparator());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int findLongestNumber(Collection<SourceContextLoader.Line> lines) {
        int maxLineNumber = 1;
        for (SourceContextLoader.Line line : lines) {
            maxLineNumber = line.getLineNumber();
        }
        return String.valueOf(maxLineNumber).length();
    }

    private void writeStringWithMaxWidth(CharSequence line, int offsetSize) throws IOException {
        int allowedSize = maxWidth - offsetSize;
        writer.append(line, 0, Math.min(line.length(), allowedSize));
        if (line.length() >= allowedSize) {
            writer.append("…");
        }
    }
}

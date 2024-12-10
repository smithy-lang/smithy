/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.util.Collection;
import java.util.Iterator;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.model.loader.sourcecontext.SourceContextLoader;

/**
 * Formats source {@link SourceContextLoader.Line} values and writes them to an {@link Appendable}.
 */
final class CodeFormatter {

    private final ColorBuffer writer;
    private final int maxWidth;

    CodeFormatter(ColorBuffer writer, int maxWidth) {
        this.writer = writer;
        this.maxWidth = maxWidth;
    }

    void writeCode(int cursorLine, int cursorColumn, Collection<SourceContextLoader.Line> lines) {
        if (lines.isEmpty()) {
            return;
        }

        // Determine the string length of the biggest number to pad the line gutter correctly.
        int numberLength = findLongestNumber(lines);

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
    }

    private void writeColumnAndContent(int numberLength, int lineNumber, CharSequence content) {
        writer.style(w -> {
            if (lineNumber == -1) {
                for (int i = 0; i < numberLength; i++) {
                    writer.append("·");
                }
                writer.append("|");
            } else {
                String lineString = String.valueOf(lineNumber);
                int thisLineLength = lineString.length();
                writer.append(lineString);
                // Write the appropriate amount of padding.
                for (int i = 0; i < numberLength - thisLineLength; i++) {
                    writer.append(' ');
                }
                writer.append("| ");
            }
        }, ColorTheme.MUTED);

        if (content.length() > 0) {
            writeStringWithMaxWidth(content, numberLength);
        }

        writer.println();
    }

    private void writePointer(int numberLength, int cursorColumn) {
        writer.style(w -> {
            for (int j = 0; j < numberLength; j++) {
                w.append(' ');
            }
            w.append("|");
        }, ColorTheme.MUTED);

        for (int j = 0; j < cursorColumn; j++) {
            writer.append(' ');
        }
        writer.print("^", ColorTheme.ERROR);
        writer.println();
    }

    private int findLongestNumber(Collection<SourceContextLoader.Line> lines) {
        int maxLineNumber = 1;
        for (SourceContextLoader.Line line : lines) {
            maxLineNumber = line.getLineNumber();
        }
        return String.valueOf(maxLineNumber).length();
    }

    private void writeStringWithMaxWidth(CharSequence line, int offsetSize) {
        int allowedSize = maxWidth - offsetSize;
        writer.append(line, 0, Math.min(line.length(), allowedSize));
        if (line.length() >= allowedSize) {
            writer.append("…");
        }
    }
}

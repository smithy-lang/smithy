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

import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Pattern;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.sourcecontext.SourceContextLoader;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventFormatter;
import software.amazon.smithy.utils.StringUtils;

final class PrettyAnsiValidationFormatter implements ValidationEventFormatter {

    private static final int LINE_LENGTH = 80;
    private static final Pattern TICK_PATTERN = Pattern.compile("`(.*?)`");
    private final SourceContextLoader sourceContextLoader;
    private final ColorFormatter colors;
    private final String rootPath = Paths.get("").normalize().toAbsolutePath().toString();

    PrettyAnsiValidationFormatter(SourceContextLoader loader, ColorFormatter colors) {
        this.sourceContextLoader = loader;
        this.colors = colors;
    }

    @Override
    public String format(ValidationEvent event) {
        String ls = System.lineSeparator();
        StringBuilder writer = new StringBuilder();
        writer.append(ls);

        switch (event.getSeverity()) {
            case WARNING:
                printTitle(writer, event, Style.YELLOW, Style.BG_YELLOW, Style.BLACK);
                break;
            case ERROR:
                printTitle(writer, event, Style.RED, Style.BG_RED, Style.BLACK);
                break;
            case DANGER:
                printTitle(writer, event, Style.MAGENTA, Style.BG_MAGENTA, Style.BLACK);
                break;
            case NOTE:
                printTitle(writer, event, Style.CYAN, Style.BG_CYAN, Style.BLACK);
                break;
            case SUPPRESSED:
            default:
                printTitle(writer, event, Style.GREEN, Style.BG_GREEN, Style.BLACK);
        }

        // Only write an event ID if there is an associated ID.
        event.getShapeId().ifPresent(id -> {
            colors.style(writer, "Shape: ", Style.BRIGHT_BLACK);
            colors.style(writer, id.toString(), Style.BLUE);
            writer.append(ls);
        });

        if (event.getSourceLocation() == SourceLocation.NONE) {
            writer.append(ls);
        } else {
            String humanReadableFilename = getHumanReadableFilename(event.getSourceLocation().getFilename());
            int line = event.getSourceLocation().getLine();
            int column = event.getSourceLocation().getColumn();
            colors.style(writer, "File:  ", Style.BRIGHT_BLACK);
            colors.style(writer, humanReadableFilename + ':' + line + ':' + column, Style.BLUE);
            writer.append(ls).append(ls);

            try {
                Collection<SourceContextLoader.Line> lines = sourceContextLoader.loadContext(event);
                if (!lines.isEmpty()) {
                    new CodeFormatter(writer, colors, LINE_LENGTH).writeCode(line, column, lines);
                }
            } catch (UncheckedIOException e) {
                colors.style(writer, "Invalid source file", Style.UNDERLINE);
                writer.append(": ");
                writeMessage(writer, e.getCause().getMessage());
                writer.append(ls).append(ls);
            }
        }

        writeMessage(writer, event.getMessage());
        writer.append(ls);

        return writer.toString();
    }

    private void printTitle(StringBuilder writer, ValidationEvent event, Style borderColor, Style... styles) {
        colors.style(writer, "── ", borderColor);
        String severity = ' ' + event.getSeverity().toString() + ' ';
        colors.style(writer, severity, styles);

        colors.style(writer, w -> {
            w.append(" ──");

            int currentLength = severity.length() + 3 + 3 + 1; // severity, dash+padding, padding+dash, padding.
            int remainingLength = LINE_LENGTH - currentLength;
            int padding = remainingLength - event.getId().length();

            for (int i = 0; i < padding; i++) {
                w.append("─");
            }

            w.append(' ');
        }, borderColor);

        writer.append(event.getId()).append(System.lineSeparator());
    }

    // Converts Markdown style ticks to use color highlights if colors are enabled.
    private void writeMessage(StringBuilder writer, String message) {
        String content = StringUtils.wrap(message, 80, System.lineSeparator(), false);

        if (colors.isColorEnabled()) {
            content = TICK_PATTERN.matcher(content).replaceAll(colors.style("$1", Style.CYAN));
        }

        writer.append(content);
    }

    private String getHumanReadableFilename(String filename) {
        if (filename.startsWith("jar:")) {
            filename = filename.substring(4);
        }

        if (filename.startsWith("file:")) {
            filename = filename.substring(5);
        }

        if (filename.startsWith(rootPath)) {
            filename = filename.substring(rootPath.length() + 1);
        }

        return filename;
    }
}

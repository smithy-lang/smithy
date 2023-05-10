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
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.sourcecontext.SourceContextLoader;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventFormatter;
import software.amazon.smithy.utils.SmithyBuilder;

final class PrettyAnsiValidationFormatter implements ValidationEventFormatter {

    private static final int LINE_LENGTH = 80;
    private final SourceContextLoader sourceContextLoader;
    private final ColorFormatter colors;
    private final String rootPath = Paths.get("").normalize().toAbsolutePath().toString();
    private final String titleLabel;
    private final Style[] titleLabelStyles;

    PrettyAnsiValidationFormatter(Builder builder) {
        this.sourceContextLoader = SmithyBuilder.requiredState("sourceContextLoader", builder.sourceContextLoader);
        this.colors = SmithyBuilder.requiredState("colors", builder.colors);
        this.titleLabel = SmithyBuilder.requiredState("titleLabel", builder.titleLabel);
        this.titleLabelStyles = builder.titleLabelStyles;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public String format(ValidationEvent event) {
        String ls = System.lineSeparator();
        try (ColorBuffer writer = ColorBuffer.of(colors, new StringBuilder())) {
            writer.append(ls);

            switch (event.getSeverity()) {
                case WARNING:
                    printTitle(writer, event, ColorTheme.WARNING, ColorTheme.WARNING_TITLE);
                    break;
                case ERROR:
                    printTitle(writer, event, ColorTheme.ERROR, ColorTheme.ERROR_TITLE);
                    break;
                case DANGER:
                    printTitle(writer, event, ColorTheme.DANGER, ColorTheme.DANGER_TITLE);
                    break;
                case NOTE:
                    printTitle(writer, event, ColorTheme.NOTE, ColorTheme.NOTE_TITLE);
                    break;
                case SUPPRESSED:
                default:
                    printTitle(writer, event, ColorTheme.SUPPRESSED, ColorTheme.SUPPRESSED_TITLE);
            }

            // Only write an event ID if there is an associated ID.
            event.getShapeId().ifPresent(id -> {
                colors.style(writer, "Shape: ", ColorTheme.MUTED);
                colors.style(writer, id.toString(), ColorTheme.EVENT_SHAPE_ID);
                writer.append(ls);
            });

            if (event.getSourceLocation() == SourceLocation.NONE) {
                writer.append(ls);
            } else {
                String humanReadableFilename = getHumanReadableFilename(event.getSourceLocation().getFilename());
                int line = event.getSourceLocation().getLine();
                int column = event.getSourceLocation().getColumn();
                colors.style(writer, "File:  ", ColorTheme.MUTED);
                colors.style(writer, humanReadableFilename + ':' + line + ':' + column, ColorTheme.MUTED);
                writer.append(ls).append(ls);

                try {
                    Collection<SourceContextLoader.Line> lines = sourceContextLoader.loadContext(event);
                    if (!lines.isEmpty()) {
                        new CodeFormatter(writer, LINE_LENGTH).writeCode(line, column, lines);
                    }
                } catch (UncheckedIOException e) {
                    colors.style(writer, "Invalid source file", ColorTheme.EM_UNDERLINE);
                    writer.append(": ");
                    writeMessage(writer, e.getCause().getMessage());
                    writer.append(ls).append(ls);
                }
            }

            writeMessage(writer, event.getMessage());
            writer.append(ls);

            return writer.toString();
        }
    }

    private void printTitle(ColorBuffer writer, ValidationEvent event, Style border, Style styles) {
        colors.style(writer, "── ", border);

        if (!titleLabel.isEmpty()) {
            colors.style(writer, ' ' + titleLabel + ' ', titleLabelStyles);
        }

        colors.style(writer, ' ' + event.getSeverity().toString() + ' ', styles);

        // dash+padding, [padding + titleLabel + padding + padding], severity, padding+dash, padding, padding.
        int prefixLength = 3 + (titleLabel.isEmpty() ? 0 : (titleLabel.length() + 2))
                           + 1 + event.getSeverity().toString().length() + 1 + 3 + 1;

        writer.style(w -> {
            w.append(" ──");
            int remainingLength = LINE_LENGTH - prefixLength;
            int padding = remainingLength - event.getId().length();
            for (int i = 0; i < padding; i++) {
                w.append("─");
            }
            w.append(' ');
        }, border);

        writer.append(event.getId()).append(System.lineSeparator());
    }

    // Converts Markdown style ticks to use color highlights if colors are enabled.
    private void writeMessage(ColorBuffer writer, String message) {
        writer.append(StyleHelper.formatMessage(message, LINE_LENGTH, colors));
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

    static final class Builder {
        private SourceContextLoader sourceContextLoader;
        private ColorFormatter colors;
        private String titleLabel = "";
        private Style[] titleLabelStyles;

        Builder sourceContextLoader(SourceContextLoader sourceContextLoader) {
            this.sourceContextLoader = sourceContextLoader;
            return this;
        }

        Builder colors(ColorFormatter colors) {
            this.colors = colors;
            return this;
        }

        Builder titleLabel(String titleLabel, Style... styles) {
            this.titleLabel = titleLabel == null ? "" : titleLabel;
            titleLabelStyles = styles;
            return this;
        }

        PrettyAnsiValidationFormatter build() {
            return new PrettyAnsiValidationFormatter(this);
        }
    }
}

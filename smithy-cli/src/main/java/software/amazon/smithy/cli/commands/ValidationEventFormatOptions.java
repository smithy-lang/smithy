/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli.commands;

import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventFormatter;

final class ValidationEventFormatOptions implements ArgumentReceiver {

    private static final String VALIDATION_FORMAT = "--format";

    enum Format {
        TEXT {
            @Override
            void print(CliPrinter printer, ValidationEventFormatter formatter, ValidationEvent event) {
                printer.println(formatter.format(event));
            }
        },

        CSV {
            @Override
            void beginPrinting(CliPrinter printer) {
                printer.println("severity,id,shape,file,line,column,message,hint,suppressionReason");
            }

            @Override
            void print(CliPrinter printer, ValidationEventFormatter formatter, ValidationEvent event) {
                printer.println(
                        String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,\"%s\",\"%s\",\"%s\"",
                                      event.getSeverity().toString(),
                                      formatCsv(event.getId()),
                                      event.getShapeId().map(ShapeId::toString).orElse(""),
                                      formatCsv(event.getSourceLocation().getFilename()),
                                      event.getSourceLocation().getLine(),
                                      event.getSourceLocation().getColumn(),
                                      formatCsv(event.getMessage()),
                                      formatCsv(event.getHint().orElse("")),
                                      formatCsv(event.getSuppressionReason().orElse(""))));
            }
        };

        void beginPrinting(CliPrinter printer) {}

        abstract void print(CliPrinter printer, ValidationEventFormatter formatter, ValidationEvent event);

        void endPrinting(CliPrinter printer) {}

        private static String formatCsv(String value) {
            // Replace DQUOTE with DQUOTEDQUOTE, escape newlines, and escape carriage returns.
            return value.replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "\\r");
        }
    }

    private Format format = Format.TEXT;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.param(VALIDATION_FORMAT, null, "text|csv",
                      "Specifies the format to write validation events (text or csv). Defaults to text.");
    }

    @Override
    public Consumer<String> testParameter(String name) {
        if (name.equals(VALIDATION_FORMAT)) {
            return s -> {
                switch (s) {
                    case "csv":
                        format(Format.CSV);
                        break;
                    case "text":
                        format(Format.TEXT);
                        break;
                    default:
                        throw new CliError("Unexpected " + VALIDATION_FORMAT + ": `" + s + "`");
                }
            };
        }
        return null;
    }

    void format(Format format) {
        this.format = format;
    }

    Format format() {
        return format;
    }
}

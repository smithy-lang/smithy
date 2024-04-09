/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.StringJoiner;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;

/**
 * Shares logic for validating a model and printing out events.
 */
final class Validator {

    enum Mode {
        QUIET, QUIET_CORE_ONLY, ENABLE;

        static Mode from(StandardOptions standardOptions) {
            return standardOptions.quiet() ? Mode.QUIET : Mode.ENABLE;
        }
    }

    private Validator() {}

    static void validate(boolean quiet, ColorFormatter colors, CliPrinter printer, ValidatedResult<Model> result) {
        int notes = result.getValidationEvents(Severity.NOTE).size();
        int warnings = result.getValidationEvents(Severity.WARNING).size();
        int errors = result.getValidationEvents(Severity.ERROR).size();
        int dangers = result.getValidationEvents(Severity.DANGER).size();
        int shapeCount = result.getResult().isPresent() ? result.getResult().get().toSet().size() : 0;
        boolean isFailed = errors > 0 || dangers > 0;
        boolean hasEvents = warnings > 0 || notes > 0 || isFailed;

        try (ColorBuffer output = ColorBuffer.of(colors, new StringBuilder())) {
            if (isFailed) {
                output.append(colors.style("FAILURE: ", ColorTheme.ERROR));
            } else {
                output.append(colors.style("SUCCESS: ", ColorTheme.SUCCESS));
            }
            output.append("Validated " + shapeCount).append(" shapes");

            if (hasEvents) {
                output.append(' ').append('(');
                StringJoiner joiner = new StringJoiner(", ");
                if (errors > 0) {
                    appendSummaryCount(joiner, "ERROR", errors);
                }

                if (dangers > 0) {
                    appendSummaryCount(joiner, "DANGER", dangers);
                }

                if (warnings > 0) {
                    appendSummaryCount(joiner, "WARNING", warnings);
                }

                if (notes > 0) {
                    appendSummaryCount(joiner, "NOTE", notes);
                }
                output.append(joiner.toString());
                output.append(')');
            }

            output.append(System.lineSeparator());

            if (!result.getResult().isPresent() || errors + dangers > 0) {
                throw new CliError(output.toString());
            } else if (!quiet) {
                printer.println(output.toString());
            }
        }
    }

    private static void appendSummaryCount(StringJoiner joiner, String label, int count) {
        joiner.add(label + ": " + count);
    }
}

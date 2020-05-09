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

package software.amazon.smithy.cli.commands;

import static java.lang.String.format;

import java.util.Set;
import java.util.function.Consumer;
import software.amazon.smithy.cli.Cli;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Colors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.ContextualValidationEventFormatter;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;

/**
 * Shares logic for validating a model and printing out events.
 */
final class Validator {

    private Validator() {}

    /**
     * Validation features.
     */
    enum Feature {
        /** Shows validation events, but does not show the summary. */
        QUIET,

        /** Writes validation events to STDOUT instead of stderr. */
        STDOUT
    }

    static void validate(ValidatedResult<Model> result, Set<Feature> features) {
        ContextualValidationEventFormatter formatter = new ContextualValidationEventFormatter();

        boolean stdout = features.contains(Feature.STDOUT);
        boolean quiet = features.contains(Feature.QUIET);
        Consumer<String> writer = stdout ? Cli.getStdout() : Cli.getStderr();

        result.getValidationEvents().stream()
                .filter(event -> event.getSeverity() != Severity.SUPPRESSED)
                .sorted()
                .forEach(event -> {
                    if (event.getSeverity() == Severity.WARNING) {
                        Colors.YELLOW.write(writer, formatter.format(event));
                    } else if (event.getSeverity() == Severity.DANGER || event.getSeverity() == Severity.ERROR) {
                        Colors.RED.write(writer, formatter.format(event));
                    } else {
                        writer.accept(event.toString());
                    }
                    writer.accept("");
                });

        long errors = result.getValidationEvents(Severity.ERROR).size();
        long dangers = result.getValidationEvents(Severity.DANGER).size();

        if (!quiet) {
            String line = format(
                    "Validation result: %s ERROR(s), %d DANGER(s), %d WARNING(s), %d NOTE(s)",
                    errors, dangers, result.getValidationEvents(Severity.WARNING).size(),
                    result.getValidationEvents(Severity.NOTE).size());
            writer.accept(line);

            result.getResult().ifPresent(model -> {
                writer.accept(String.format("Validated %d shapes in model", model.shapes().count()));
            });
        }

        if (!result.getResult().isPresent() || errors + dangers > 0) {
            // Show the error and danger severity events.
            throw new CliError(format("The model is invalid: %s ERROR(s), %d DANGER(s)", errors, dangers));
        }
    }
}

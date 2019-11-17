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

import java.util.Collections;
import java.util.Comparator;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Colors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Shares logic for validating a model and printing out events.
 */
final class Validator {
    private Validator() {}

    static void validate(ValidatedResult<Model> result) {
        validate(result, false);
    }

    static void validate(ValidatedResult<Model> result, boolean quiet) {
        result.getValidationEvents().stream()
                .filter(event -> event.getSeverity() != Severity.SUPPRESSED)
                .sorted(Comparator.comparing(ValidationEvent::toString))
                .forEach(event -> {
                    if (event.getSeverity() == Severity.WARNING) {
                        Colors.out(Colors.YELLOW, event.toString());
                    } else if (event.getSeverity() == Severity.DANGER || event.getSeverity() == Severity.ERROR) {
                        Colors.out(Colors.RED, event.toString());
                    } else {
                        System.out.println(event);
                    }
                });

        long errors = result.getValidationEvents(Severity.ERROR).size();
        long dangers = result.getValidationEvents(Severity.DANGER).size();

        if (!quiet) {
            String line = format(
                    "Validation result: %s ERROR(s), %d DANGER(s), %d WARNING(s), %d NOTE(s)",
                    errors, dangers, result.getValidationEvents(Severity.WARNING).size(),
                    result.getValidationEvents(Severity.NOTE).size());
            System.out.println(String.join("", Collections.nCopies(line.length(), "-")));
            System.out.println(line);
            result.getResult().ifPresent(model -> System.out.println(String.format(
                    "Validated %d shapes in model", model.shapes().count())));
        }

        if (errors + dangers > 0) {
            // Show the error and danger severity events.
            throw new CliError(format("The model is invalid: %s ERROR(s), %d DANGER(s)", errors, dangers));
        }
    }
}

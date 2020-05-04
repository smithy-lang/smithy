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

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Cli;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Colors;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.Parser;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class DiffCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(DiffCommand.class.getName());

    @Override
    public String getName() {
        return "diff";
    }

    @Override
    public String getSummary() {
        return "Diffs two Smithy models and reports any significant changes";
    }

    @Override
    public Parser getParser() {
        return Parser.builder()
                .repeatedParameter("--old", "Path to an old Smithy model or directory that contains models")
                .repeatedParameter("--new", "Path to the new Smithy model or directory that contains models")
                .build();
    }

    @Override
    public void execute(Arguments arguments, ClassLoader classLoader) {
        List<String> oldModels = arguments.repeatedParameter("--old");
        LOGGER.info(String.format("Setting 'old' Smithy models: %s", String.join(" ", oldModels)));
        List<String> newModels = arguments.repeatedParameter("--new");
        LOGGER.info(String.format("Setting 'new' Smithy models: %s", String.join(" ", newModels)));

        ModelAssembler assembler = CommandUtils.createModelAssembler(classLoader);
        Model oldModel = loadModel("old", assembler, oldModels);
        assembler.reset();
        Model newModel = loadModel("new", assembler, newModels);

        List<ValidationEvent> events = ModelDiff.compare(classLoader, oldModel, newModel);
        boolean hasError = events.stream().anyMatch(event -> event.getSeverity() == Severity.ERROR);
        boolean hasDanger = events.stream().anyMatch(event -> event.getSeverity() == Severity.DANGER);
        boolean hasWarning = events.stream().anyMatch(event -> event.getSeverity() == Severity.DANGER);
        String result = events.stream().map(ValidationEvent::toString).collect(Collectors.joining("\n"));

        if (hasError) {
            throw new CliError(String.format("Model diff detected errors: %n%s", result));
        }

        if (!result.isEmpty()) {
            Cli.stdout(result);
        }

        if (hasDanger) {
            Colors.BRIGHT_BOLD_RED.out("Smithy diff detected danger");
        } else if (hasWarning) {
            Colors.BRIGHT_BOLD_YELLOW.out("Smithy diff complete with warnings");
        } else {
            Colors.BRIGHT_BOLD_GREEN.out("Smithy diff complete");
        }
    }

    private Model loadModel(String descriptor, ModelAssembler assembler, List<String> models) {
        models.forEach(assembler::addImport);
        ValidatedResult<Model> result = assembler.assemble();
        if (result.isBroken()) {
            throw new CliError("Error loading " + descriptor + " models: \n" + result.getValidationEvents().stream()
                    .map(ValidationEvent::toString)
                    .collect(Collectors.joining("\n")));
        }

        return result.unwrap();
    }
}

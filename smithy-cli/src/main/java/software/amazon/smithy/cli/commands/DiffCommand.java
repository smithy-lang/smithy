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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

final class DiffCommand extends ClasspathCommand {
    private static final Logger LOGGER = Logger.getLogger(DiffCommand.class.getName());

    DiffCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        super(parentCommandName, dependencyResolverFactory);
    }

    @Override
    public String getName() {
        return "diff";
    }

    @Override
    public String getSummary() {
        return "Compares two Smithy models and reports any significant changes.";
    }

    private static final class Options implements ArgumentReceiver {
        private final List<String> oldModels = new ArrayList<>();
        private final List<String> newModels = new ArrayList<>();

        @Override
        public boolean testOption(String name) {
            return false;
        }

        @Override
        public Consumer<String> testParameter(String name) {
            if (name.equals("--old")) {
                return oldModels::add;
            } else if (name.equals("--new")) {
                return newModels::add;
            } else {
                return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--old", null, "OLD_MODELS...",
                          "Path to an old Smithy model or directory that contains models. This option can be "
                          + "repeated to merge multiple files or directories "
                          + "(e.g., --old /path/old/one --old /path/old/two).");
            printer.param("--new", null, "NEW_MODELS...",
                          "Path to the new Smithy model or directory that contains models. This option can be "
                          + "repeated to merge multiple files or directories."
                          + "(e.g., --new /path/new/one --new /path/new/two)");
        }
    }

    @Override
    protected void configureArgumentReceivers(Arguments arguments) {
        super.configureArgumentReceivers(arguments);
        arguments.addReceiver(new Options());
    }

    @Override
    int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env, List<String> positional) {
        StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);
        Options options = arguments.getReceiver(Options.class);
        ClassLoader classLoader = env.classLoader();

        List<String> oldModels = options.oldModels;
        List<String> newModels = options.newModels;
        LOGGER.fine(() -> String.format("Setting old models to: %s; new models to: %s", oldModels, newModels));

        ModelAssembler assembler = ModelBuilder.createModelAssembler(classLoader);
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
            env.stdout().println(result);
        }

        // Print the "framing" style output to stderr only if !quiet.
        if (!standardOptions.quiet()) {
            try (ColorFormatter.PrinterBuffer buffer = env.colors().printerBuffer(env.stderr())) {
                if (hasDanger) {
                    buffer.println("Smithy diff detected danger", Style.BRIGHT_RED, Style.BOLD);
                } else if (hasWarning) {
                    buffer.println("Smithy diff detected warnings", Style.BRIGHT_YELLOW, Style.BOLD);
                } else {
                    buffer.println("Smithy diff complete", Style.BRIGHT_GREEN, Style.BOLD);
                }
            }
        }

        return 0;
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

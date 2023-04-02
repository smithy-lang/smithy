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
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

final class DiffCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(DiffCommand.class.getName());
    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    DiffCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    public String getName() {
        return "diff";
    }

    @Override
    public String getSummary() {
        return "Compares two Smithy models and reports differences.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new DiscoveryOptions());
        arguments.addReceiver(new SeverityOption());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new Options());
        arguments.getReceiver(BuildOptions.class).noPositionalArguments(true);

        CommandAction action = HelpActionWrapper.fromCommand(
                this, parentCommandName, new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader));

        return action.apply(arguments, env);
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
                          + "(e.g., --new /path/new/one --new /path/new/two).");
        }
    }

    int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env) {
        if (!arguments.getPositional().isEmpty()) {
            throw new CliError("Unexpected arguments: " + arguments.getPositional());
        }

        Options options = arguments.getReceiver(Options.class);
        ClassLoader classLoader = env.classLoader();

        List<String> oldModels = options.oldModels;
        List<String> newModels = options.newModels;
        LOGGER.fine(() -> String.format("Setting old models to: %s; new models to: %s", oldModels, newModels));

        ModelBuilder modelBuilder = new ModelBuilder()
                .config(config)
                .arguments(arguments)
                .env(env)
                .validationPrinter(env.stderr())
                .validationMode(Validator.Mode.DISABLE)
                .severity(Severity.DANGER);
        Model oldModel = modelBuilder
                .models(oldModels)
                .titleLabel("OLD", ColorTheme.DIFF_EVENT_TITLE)
                .build();
        Model newModel = modelBuilder
                .models(newModels)
                .titleLabel("NEW", ColorTheme.DIFF_EVENT_TITLE)
                .build();

        // Diff the models and report on the events, failing if necessary.
        List<ValidationEvent> events = ModelDiff.compare(classLoader, oldModel, newModel);
        modelBuilder
                .titleLabel("DIFF", ColorTheme.DIFF_TITLE)
                .validatedResult(new ValidatedResult<>(newModel, events))
                .severity(null) // reset so it takes on standard option settings.
                .build();

        return 0;
    }
}

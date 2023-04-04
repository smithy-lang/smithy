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

import java.util.Collections;
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
        arguments.addReceiver(new SeverityOption());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new Options());
        arguments.getReceiver(BuildOptions.class).noPositionalArguments(true);

        CommandAction action = HelpActionWrapper.fromCommand(
                this, parentCommandName, new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader));

        return action.apply(arguments, env);
    }

    private static final class Options implements ArgumentReceiver {
        private String oldModel;
        private String newModel;

        @Override
        public boolean testOption(String name) {
            return false;
        }

        @Override
        public Consumer<String> testParameter(String name) {
            if (name.equals("--old")) {
                return m -> oldModel = m;
            } else if (name.equals("--new")) {
                return n -> newModel = n;
            } else {
                return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--old", null, "OLD_MODEL",
                          "Path to an old Smithy model file or directory that contains model files.");
            printer.param("--new", null, "NEW_MODEL",
                          "Path to the new Smithy model file or directory that contains model files.");
        }
    }

    int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);

        if (!arguments.getPositional().isEmpty()) {
            throw new CliError("Unexpected arguments: " + arguments.getPositional());
        }

        // TODO: Add checks here for `mode` to change the DiffMode. Defaults to arbitrary.
        return DiffMode.ARBITRARY.diff(config, arguments, options, env);
    }

    private enum DiffMode {
        ARBITRARY {
            @Override
            int diff(SmithyBuildConfig config, Arguments arguments, Options options, Env env) {
                String oldModelOpt = options.oldModel;
                if (oldModelOpt == null) {
                    throw new CliError("Missing required --old argument");
                }

                String newModelOpt = options.newModel;
                if (newModelOpt == null) {
                    throw new CliError("Missing required --new argument");
                }

                LOGGER.fine(() -> String.format("Setting old models to: %s; new models to: %s",
                                                oldModelOpt, newModelOpt));

                ModelBuilder modelBuilder = new ModelBuilder()
                        .config(config)
                        .arguments(arguments)
                        .env(env)
                        .validationPrinter(env.stderr())
                        // Don't use imports or sources from the model config file.
                        .disableConfigModels(true)
                        // Only report issues that fail the build.
                        .validationMode(Validator.Mode.DISABLE)
                        .severity(Severity.DANGER);

                // Use the ModelBuilder template to build the old model.
                Model oldModel = modelBuilder
                        .models(Collections.singletonList(oldModelOpt))
                        .titleLabel("OLD", ColorTheme.DIFF_EVENT_TITLE)
                        .build();

                // Use the same ModelBuilder template to build the new model.
                Model newModel = modelBuilder
                        .models(Collections.singletonList(newModelOpt))
                        .titleLabel("NEW", ColorTheme.DIFF_EVENT_TITLE)
                        .build();

                // Diff the models and report on the events, failing if necessary.
                // We *do* use dependencies in smithy-build.json (if present) to find custom diff evaluators.
                ClassLoader classLoader = env.classLoader();
                List<ValidationEvent> events = ModelDiff.compare(classLoader, oldModel, newModel);
                modelBuilder
                        .titleLabel("DIFF", ColorTheme.DIFF_TITLE)
                        .validatedResult(new ValidatedResult<>(newModel, events))
                        .severity(null) // reset so it takes on standard option settings.
                        .build();

                return 0;
            }
        };

        abstract int diff(SmithyBuildConfig config, Arguments arguments, Options options, Env env);
    }
}

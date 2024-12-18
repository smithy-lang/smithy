/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

final class DiffCommand implements Command {

    private static final String DIFF_WORKTREE_BRANCH = "__smithy-diff-worktree";
    private static final String DIFF_WORKTREE_PATH = "diff-worktree";
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
        arguments.addReceiver(new ValidatorOptions());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new ValidationEventFormatOptions());
        arguments.addReceiver(new Options());
        arguments.getReceiver(BuildOptions.class).noPositionalArguments(true);

        CommandAction action = HelpActionWrapper.fromCommand(
                this,
                parentCommandName,
                this::getDocumentation,
                new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader));

        return action.apply(arguments, env);
    }

    private String getDocumentation(ColorFormatter colors) {
        String ls = System.lineSeparator();
        String content =
                "The `diff` command supports different modes through the `--mode` option:"
                        + ls
                        + ls
                        + "`--mode arbitrary`:"
                        + ls
                        + "Compares two arbitrary models. This mode requires that `--old` and `--new` are specified. "
                        + "When run within a project directory that contains a `smithy-build.json` config, any dependencies "
                        + "defined in the config file are used when loading both the old and new models; however, `imports` "
                        + "and `sources` defined in the config file are not used. This is the default mode when no `--mode` "
                        + "is specified and `--old` or `--new` are provided."
                        + ls
                        + ls
                        + "    smithy diff --old /path/old --new /path/new"
                        + ls
                        + "    smithy diff --mode arbitrary --old /path/old --new /path/new"
                        + ls
                        + ls
                        + "`--mode project`:"
                        + ls
                        + "Compares the current state of a project against another project. `--old` is required and points "
                        + "to the location of another Smithy model or the root directory of another project. `--new` is not "
                        + "allowed in this mode because the new model is the project in the current working directory. The "
                        + "old model does not use any `sources` or `imports` defined by the current project, though it is "
                        + "loaded using any dependencies defined by the current project. If the `--old` argument points to "
                        + "a directory that contains a `smithy-build.json` file, any `imports` or `sources` defined in that "
                        + "config file will be used when loading the old model, though the dependencies of the old model "
                        + "are ignored."
                        + ls
                        + ls
                        + "    smithy diff --mode project --old /path/old"
                        + ls
                        + ls
                        + "`--mode git`:"
                        + ls
                        + "Compares the current state of a Smithy project to another commit in the current git repo. This "
                        + "command must be run from within a git repo. The `--old` argument can be provided to specify a "
                        + "specific revision to compare against. If `--old` is not provided, the commit defaults to `HEAD` "
                        + "(the last commit on the current branch). This mode is a wrapper around `--mode project`, so its "
                        + "restrictions apply. This is the default mode when no arguments are provided."
                        + ls
                        + ls
                        + "    # Equivalent to `smithy diff --mode git`"
                        + "    smithy diff"
                        + ls
                        + "    smithy diff --mode git"
                        + ls
                        + "    smithy diff --mode git --old main"
                        + ls
                        + "    smithy diff --mode git --old HEAD~2";
        return StyleHelper.markdownLiterals(content, colors);
    }

    private static final class Options implements ArgumentReceiver {
        private DiffMode diffMode = DiffMode.DETECTED;
        private String oldModel;
        private String newModel;

        @Override
        public boolean testOption(String name) {
            return false;
        }

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--old":
                    return m -> oldModel = m;
                case "--new":
                    return n -> newModel = n;
                case "--mode":
                    return m -> {
                        try {
                            diffMode = DiffMode.valueOf(m.toUpperCase(Locale.ENGLISH));
                        } catch (IllegalArgumentException e) {
                            throw new CliError("Invalid --diff mode provided: " + m);
                        }
                    };
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--mode",
                    null,
                    "DIFF_MODE",
                    "The diff mode to use: 'arbitrary' (the default mode), 'project', 'git'.");
            printer.param("--old",
                    null,
                    "OLD_MODEL",
                    "Path to an old Smithy model file or directory that contains model files. When using "
                            + "git mode, this argument refers to a Git commit or branch.");
            printer.param("--new",
                    null,
                    "NEW_MODEL",
                    "Path to the new Smithy model file or directory that contains model files. This argument "
                            + "is not allowed in project or git mode.");
        }
    }

    int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);

        if (!arguments.getPositional().isEmpty()) {
            throw new CliError("Unexpected arguments: " + arguments.getPositional());
        }

        return options.diffMode.diff(config, arguments, options, env);
    }

    private enum DiffMode {
        DETECTED {
            @Override
            int diff(SmithyBuildConfig config, Arguments arguments, Options options, Env env) {
                if (options.oldModel != null || options.newModel != null) {
                    return ARBITRARY.diff(config, arguments, options, env);
                } else {
                    return GIT.diff(config, arguments, options, env);
                }
            }
        },

        ARBITRARY {
            @Override
            int diff(SmithyBuildConfig config, Arguments arguments, Options options, Env env) {
                if (options.oldModel == null) {
                    throw new CliError("Missing required --old argument");
                }

                if (options.newModel == null) {
                    throw new CliError("Missing required --new argument");
                }

                ModelBuilder modelBuilder = createModelBuilder(config, arguments, env);
                // Don't use imports or sources from the model config file.
                modelBuilder.disableConfigModels(true);

                // Use the ModelBuilder template to build the old model.
                Model oldModel = modelBuilder
                        .models(Collections.singletonList(options.oldModel))
                        .titleLabel("OLD", ColorTheme.DIFF_EVENT_TITLE)
                        .build();

                // Use the same ModelBuilder template to build the new model, being careful to use the original config.
                Model newModel = createNewModel(modelBuilder, Collections.singletonList(options.newModel), config);
                runDiff(modelBuilder, env, oldModel, newModel);
                return 0;
            }
        },

        PROJECT {
            @Override
            int diff(SmithyBuildConfig config, Arguments arguments, Options options, Env env) {
                if (options.oldModel == null) {
                    throw new CliError("Missing required --old argument");
                }

                if (options.newModel != null) {
                    throw new CliError("--new cannot be used with this diff mode");
                }

                ModelBuilder modelBuilder = createModelBuilder(config, arguments, env);

                // Use the ModelBuilder template to build the old model.
                // The old model is built as if we're rooted in the given directory, allowing ConfigOptions to look
                // for a smithy-build.json file, which can then use imports and sources.
                ConfigOptions oldConfig = new ConfigOptions();
                oldConfig.root(Paths.get(options.oldModel));

                Model oldModel = modelBuilder
                        .models(Collections.emptyList())
                        .config(oldConfig.createSmithyBuildConfig())
                        .titleLabel("OLD", ColorTheme.DIFF_EVENT_TITLE)
                        .build();

                // Use the same ModelBuilder template to build the new model, being careful to use the original config.
                Model newModel = createNewModel(modelBuilder, Collections.emptyList(), config);
                runDiff(modelBuilder, env, oldModel, newModel);
                return 0;
            }
        },

        GIT {
            @Override
            int diff(SmithyBuildConfig config, Arguments arguments, Options options, Env env) {
                // Note: newModel is validated in PROJECT. Old model defaults to HEAD of current branch.
                if (options.oldModel == null) {
                    options.oldModel = "HEAD";
                }

                if (!arguments.getReceiver(StandardOptions.class).quiet()) {
                    env.colors()
                            .println(env.stderr(),
                                    "Comparing current project to git " + options.oldModel,
                                    ColorTheme.MUTED);
                }

                // Setup a worktree if one isn't present.
                Path outputRoot = arguments.getReceiver(BuildOptions.class).resolveOutput(config);
                Path worktreePath = outputRoot.resolve(DIFF_WORKTREE_PATH);
                Path root = Paths.get(".");

                // Determine the SHA of the given --old branch in the root git directory.
                String sha = getSha(root, options.oldModel);

                if (!Files.isDirectory(worktreePath)) {
                    // First, prune old work trees in case smithy clean was run and left a prunable diff worktree.
                    exec(ListUtils.of("git", "worktree", "prune"), root, "Error pruning worktrees");
                    // Now create the worktree using a dedicated branch. The branch allows other worktrees to checkout
                    // the same branch or SHA without conflicting.
                    exec(ListUtils.of("git",
                            "worktree",
                            "add",
                            "--quiet",
                            "--force",
                            "-B",
                            DIFF_WORKTREE_BRANCH,
                            worktreePath.toString(),
                            sha),
                            root,
                            "Unable to create git worktree");
                } else {
                    // Checkout the right commit in the worktree.
                    exec(ListUtils.of("git", "reset", "--quiet", "--hard", sha),
                            worktreePath,
                            "Unable to checkout " + options.oldModel + " in git worktree");
                }

                // Now run a project mode build using the worktree.
                options.diffMode = PROJECT;
                options.oldModel = worktreePath.toString();
                return PROJECT.diff(config, arguments, options, env);
            }

            private String getSha(Path root, String commitish) {
                // Determine the SHA of the given --old branch in the root git directory.
                List<String> args = ListUtils.of("git", "rev-parse", commitish);
                return exec(args, root, "Invalid git revision '" + commitish + "'").trim();
            }
        };

        // Create a ModelBuilder template to load the old, then new, then diff both.
        protected final ModelBuilder createModelBuilder(SmithyBuildConfig config, Arguments arguments, Env env) {
            return new ModelBuilder()
                    .config(config)
                    .arguments(arguments)
                    .env(env)
                    .validationPrinter(env.stdout())
                    // Only report issues that fail the build.
                    .validationMode(Validator.Mode.QUIET_CORE_ONLY)
                    .defaultSeverity(Severity.DANGER);
        }

        // Creating a new model is the same for each diff mode.
        protected final Model createNewModel(ModelBuilder builder, List<String> models, SmithyBuildConfig config) {
            return builder
                    .models(models)
                    .titleLabel("NEW", ColorTheme.DIFF_EVENT_TITLE)
                    .config(config)
                    .disableOutputFormatFraming(true) // don't repeat things like CSV headers.
                    .build();
        }

        // Running the diff is the same for each diff mode.
        protected final void runDiff(ModelBuilder builder, Env env, Model oldModel, Model newModel) {
            ClassLoader classLoader = env.classLoader();
            List<ValidationEvent> events = ModelDiff.compare(classLoader, oldModel, newModel);
            builder
                    .titleLabel("DIFF", ColorTheme.DIFF_TITLE)
                    .validatedResult(new ValidatedResult<>(newModel, events))
                    .defaultSeverity(null) // reset so it takes on standard option settings.
                    .disableOutputFormatFraming(true) // don't repeat things like CSV headers.
                    .build();
        }

        abstract int diff(SmithyBuildConfig config, Arguments arguments, Options options, Env env);
    }

    private static String exec(List<String> args, Path root, String errorPrefix) {
        StringBuilder output = new StringBuilder();
        int code = IoUtils.runCommand(args, root, output, Collections.emptyMap());
        if (code != 0) {
            throw new CliError(errorPrefix + ": " + output);
        }
        return output.toString();
    }
}

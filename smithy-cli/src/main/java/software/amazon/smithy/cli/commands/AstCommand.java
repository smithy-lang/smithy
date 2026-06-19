/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;

final class AstCommand implements Command {

    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    AstCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    public String getName() {
        return "ast";
    }

    @Override
    public String getSummary() {
        return "Reads Smithy models in and writes out a single JSON AST model.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new Options());

        // --name reads a pre-built, pre-validated registration, so it must NOT go through ClasspathAction
        // (which loads a cwd smithy-build.json and resolves Maven deps) and can't be combined with the
        // filesystem-model-loading flags. The branch is decided after parsing, since --name is consumed by
        // the receiver during parsing. Mirrors SelectCommand.
        ClasspathAction classpathAction = new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader);
        CommandAction action = HelpActionWrapper.fromCommand(
                this,
                parentCommandName,
                (args, e) -> args.getReceiver(Options.class).name != null
                        ? runWithRegistration(args, e)
                        : classpathAction.apply(args, e));

        return action.apply(arguments, env);
    }

    private static final class Options implements ArgumentReceiver {
        static final String FLATTEN_OPTION = "--flatten";
        private boolean flatten = false;

        static final String INCLUDE_PRELUDE_OPTION = "--include-prelude";
        private boolean includePrelude = false;

        static final String NO_DOCS_OPTION = "--no-docs";
        private boolean noDocs = false;

        private String name;
        private Selector selector;

        @Override
        public boolean testOption(String name) {
            if (FLATTEN_OPTION.equals(name)) {
                flatten = true;
                return true;
            }
            if (INCLUDE_PRELUDE_OPTION.equals(name)) {
                includePrelude = true;
                return true;
            }
            if (NO_DOCS_OPTION.equals(name)) {
                noDocs = true;
                return true;
            }
            return false;
        }

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--name":
                    return value -> this.name = value;
                case "--selector":
                    return value -> this.selector = Selector.parse(value);
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--name",
                    null,
                    "NAME",
                    "Read a service registered with `smithy register` by name, instead of loading models from "
                            + "the filesystem. Cannot be combined with model paths, --config, or --discover "
                            + "(the registered model is already built and validated).");
            printer.param("--selector",
                    null,
                    "SELECTOR",
                    "Emit only the shapes matched by a Smithy selector (and nothing else), rather than the whole "
                            + "model. For an operation's full closure, use "
                            + "'[id = ns#Operation] :is(*, ~>)'.");
            printer.option(FLATTEN_OPTION, null, "Flattens and removes mixins from the model.");
            printer.option(INCLUDE_PRELUDE_OPTION, null, "Includes the prelude shapes in the model.");
            printer.option(NO_DOCS_OPTION,
                    null,
                    "Strips documentation, externalDocumentation, and examples traits. Useful for a compact "
                            + "structure-only model (e.g. for an LLM); AWS docs can dominate the output.");
        }
    }

    private int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env) {
        Model model = new ModelBuilder()
                .config(config)
                .arguments(arguments)
                .env(env)
                .models(arguments.getPositional())
                .validationPrinter(env.stderr())
                .validationMode(Validator.Mode.QUIET)
                .defaultSeverity(Severity.DANGER)
                .build();
        return serialize(model, arguments, env);
    }

    /**
     * Reads a model registered with {@code smithy register}, loaded from its pre-built (with-docs)
     * artifact. The artifact is already assembled and validated, so the filesystem model-loading flags
     * don't apply and are rejected rather than silently ignored. Mirrors SelectCommand.
     */
    private int runWithRegistration(Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);
        rejectFilesystemFlags(arguments);

        Path artifact = CallProfiles.modelArtifact(options.name);
        if (!Files.isRegularFile(artifact)) {
            throw new CliError("'" + options.name + "' is not a registered service (no built model artifact). "
                    + "See `smithy register --list`.");
        }
        Model model = CallArtifacts.loadArtifactModel(artifact, env.classLoader());
        return serialize(model, arguments, env);
    }

    /** Errors if any filesystem model-loading flag is combined with --name (they'd be ignored). */
    private void rejectFilesystemFlags(Arguments arguments) {
        if (!arguments.getPositional().isEmpty()) {
            throw new CliError("--name cannot be combined with model paths: the registered model is used.");
        }
        if (arguments.getReceiver(ConfigOptions.class).hasExplicitConfig()) {
            throw new CliError("--name cannot be combined with --config: the registered model is used.");
        }
    }

    private int serialize(Model model, Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);
        if (options.flatten) {
            model = ModelTransformer.create().flattenAndRemoveMixins(model);
        }
        // Strip documentation/externalDocumentation/examples (the same traits `smithy register` removes
        // for its no-docs artifact). AWS docs are large HTML blobs that dominate the output; dropping them
        // yields a compact structure-only model. Done before selector filtering so the closure is lean too.
        if (options.noDocs) {
            model = ModelTransformer.create()
                    .removeTraitsIf(model,
                            (shape, trait) -> trait.toShapeId().equals(DocumentationTrait.ID)
                                    || trait.toShapeId().equals(ExternalDocumentationTrait.ID)
                                    || trait.toShapeId().equals(ExamplesTrait.ID));
        }
        // A selector scopes the output to exactly the matched shapes (and nothing else). Unlike
        // `smithy select`, which prints shape IDs, this emits the matched shapes as a full JSON model, so
        // an operation closure selector yields a self-contained sub-model that can be reasoned about
        // standalone.
        if (options.selector != null) {
            Set<Shape> matched = options.selector.select(model);
            Model.Builder builder = Model.builder();
            for (Shape shape : matched) {
                builder.addShape(shape);
            }
            model = builder.build();
        }
        ModelSerializer serializer = ModelSerializer.builder().includePrelude(options.includePrelude).build();
        env.stdout().println(Node.prettyPrintJson(serializer.serialize(model)));
        return 0;
    }
}

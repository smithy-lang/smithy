/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.smf.SmfWriter;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;

final class TransformCommand implements Command {

    // Traits not needed to execute a call; split into the cold-trait table for
    // the dynamic-client profile so selective loads can skip decoding them.
    private static final Set<String> COLD_TRAITS = new HashSet<>(Arrays.asList(
            "smithy.api#documentation",
            "smithy.api#examples",
            "smithy.rules#endpointTests"));

    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    TransformCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    public String getName() {
        return "transform";
    }

    @Override
    public String getSummary() {
        return "Transforms a Smithy model to JSON AST and SMF formats.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new Options());

        CommandAction action = HelpActionWrapper.fromCommand(
                this,
                parentCommandName,
                new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader));

        return action.apply(arguments, env);
    }

    private static final class Options implements ArgumentReceiver {
        private static final String FORMAT_SHORT = "-f";
        private static final String FORMAT_LONG = "--format";
        private static final String OUTPUT_OPTION = "--output";
        private static final String FLATTEN_OPTION = "--flatten";
        private static final String SPLIT_COLD_TRAITS_OPTION = "--split-cold-traits";

        private String format = "ast";
        private String output;
        private boolean flatten = false;
        private boolean splitColdTraits = false;

        @Override
        public boolean testOption(String name) {
            if (FLATTEN_OPTION.equals(name)) {
                flatten = true;
                return true;
            }
            if (SPLIT_COLD_TRAITS_OPTION.equals(name)) {
                splitColdTraits = true;
                return true;
            }
            return false;
        }

        @Override
        public Consumer<String> testParameter(String name) {
            if (FORMAT_SHORT.equals(name) || FORMAT_LONG.equals(name)) {
                return value -> format = value;
            }
            if (OUTPUT_OPTION.equals(name)) {
                return value -> output = value;
            }
            return null;
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param(FORMAT_LONG,
                    FORMAT_SHORT,
                    "FORMAT",
                    "Output format: ast (JSON AST, default) or smf (binary).");
            printer.param(OUTPUT_OPTION,
                    null,
                    "DIR",
                    "Output directory. Defaults to build/smithy.");
            printer.option(FLATTEN_OPTION, null, "Flattens and removes mixins from the model.");
            printer.option(SPLIT_COLD_TRAITS_OPTION,
                    null,
                    "For smf output, split documentation/examples/endpoint-test traits into a "
                            + "separate cold-trait table so selective (dynamic-client) loads can skip "
                            + "them. Produces the dynamic-client profile.");
        }
    }

    private int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env) {
        List<String> models = arguments.getPositional();
        Model model = new ModelBuilder()
                .config(config)
                .arguments(arguments)
                .env(env)
                .models(models)
                .validationPrinter(env.stderr())
                .validationMode(Validator.Mode.QUIET)
                .defaultSeverity(Severity.DANGER)
                .build();

        Options options = arguments.getReceiver(Options.class);
        if (options.flatten) {
            model = ModelTransformer.create().flattenAndRemoveMixins(model);
        }

        Path dir = Paths.get(options.output != null ? options.output : "build/smithy");

        switch (options.format) {
            case "ast":
                String json = Node.prettyPrintJson(ModelSerializer.builder().build().serialize(model));
                env.stdout().println(json);
                writeTo(dir.resolve("model.json"), json.getBytes(StandardCharsets.UTF_8));
                env.stderr().println("Wrote " + dir.resolve("model.json"));
                break;
            case "smf":
                Set<Path> sourcePaths = collectSourcePaths(config, models);
                SmfWriter.Builder writerBuilder = SmfWriter.builder()
                        .shapeFilter(shape -> isSourceShape(shape, sourcePaths));
                if (options.splitColdTraits) {
                    writerBuilder.splitColdTraits(true)
                            .coldTraitFilter(trait -> COLD_TRAITS.contains(trait.toShapeId().toString()));
                }
                byte[] data = writerBuilder.build().serialize(model);
                writeTo(dir.resolve("model.smf"), data);
                env.stderr().println("Wrote " + dir.resolve("model.smf"));
                break;
            default:
                throw new CliError("Unknown format: " + options.format
                        + ". Expected 'ast' or 'smf'.");
        }
        return 0;
    }

    private static Set<Path> collectSourcePaths(SmithyBuildConfig config, List<String> models) {
        Set<Path> sourcePaths = new HashSet<>();
        for (String m : models) {
            sourcePaths.add(Paths.get(m).toAbsolutePath().normalize());
        }
        for (String s : config.getSources()) {
            sourcePaths.add(Paths.get(s).toAbsolutePath().normalize());
        }
        for (String i : config.getImports()) {
            sourcePaths.add(Paths.get(i).toAbsolutePath().normalize());
        }
        return sourcePaths;
    }

    private static boolean isSourceShape(Shape shape, Set<Path> sourcePaths) {
        String filename = shape.getSourceLocation().getFilename();
        if (filename.isEmpty() || filename.equals(SourceLocation.NONE.getFilename())) {
            return true; // shapes without a source location are assumed to be user-defined
        }
        // Shapes from JARs are never source shapes
        if (filename.contains("!/")) {
            return false;
        }
        // Check if the shape's file is under one of the source paths, using
        // path-segment-aware comparison so that (e.g.) /foo does not match
        // /foobar.
        Path absFilename = Paths.get(filename).toAbsolutePath().normalize();
        for (Path sourcePath : sourcePaths) {
            if (absFilename.equals(sourcePath) || absFilename.startsWith(sourcePath)) {
                return true;
            }
        }
        return false;
    }

    private void writeTo(Path path, byte[] data) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, data);
        } catch (IOException e) {
            throw new CliError("Failed to write to " + path + ": " + e.getMessage());
        }
    }
}

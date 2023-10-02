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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.ProjectionResult;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.sourcecontext.SourceContextLoader;
import software.amazon.smithy.model.validation.Severity;

final class BuildCommand implements Command {

    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    BuildCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    public String getName() {
        return "build";
    }

    @Override
    public String getSummary() {
        return "Builds Smithy models and creates plugin artifacts for each projection found in smithy-build.json.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new DiscoveryOptions());
        arguments.addReceiver(new SeverityOption());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new Options());

        CommandAction action = HelpActionWrapper.fromCommand(
                this, parentCommandName, new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader));

        return action.apply(arguments, env);
    }

    private static final class Options implements ArgumentReceiver {
        private String projection;
        private String plugin;

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--projection":
                    return value -> projection = value;
                case "--plugin":
                    return value -> plugin = value;
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--projection", null, "PROJECTION_NAME", "Only generate artifacts for this projection.");
            printer.param("--plugin", null, "PLUGIN_NAME", "Only generate artifacts for this plugin.");
        }
    }

    private int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env) {
        List<String> models = arguments.getPositional();
        Options options = arguments.getReceiver(Options.class);
        BuildOptions buildOptions = arguments.getReceiver(BuildOptions.class);
        StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);
        ClassLoader classLoader = env.classLoader();
        Model model = new ModelBuilder()
                .config(config)
                .arguments(arguments)
                .env(env)
                .models(models)
                .validationPrinter(env.stderr())
                .build();

        if (!standardOptions.quiet()) {
            env.colors().println(env.stderr(), "Validated model, now starting projections...", ColorTheme.MUTED);
            env.stderr().println("");
        }

        Supplier<ModelAssembler> modelAssemblerSupplier = () -> {
            ModelAssembler assembler = Model.assembler(classLoader);
            if (buildOptions.allowUnknownTraits()) {
                assembler.putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
            }
            return assembler;
        };
        SmithyBuild smithyBuild = SmithyBuild.create(classLoader, modelAssemblerSupplier)
                .config(config)
                .model(model);

        if (buildOptions.output() != null) {
            smithyBuild.outputDirectory(buildOptions.output());
        }

        if (options.plugin != null) {
            smithyBuild.pluginFilter(name -> name.equals(options.plugin));
        }

        if (options.projection != null) {
            smithyBuild.projectionFilter(name -> name.equals(options.projection));
        }

        // Register sources with the builder.
        models.forEach(path -> smithyBuild.registerSources(Paths.get(path)));

        ResultConsumer resultConsumer = new ResultConsumer(env.colors(), env.stderr(), standardOptions.quiet());
        smithyBuild.build(resultConsumer, resultConsumer);

        env.flush();

        if (!standardOptions.quiet()) {
            try (ColorBuffer buffer = ColorBuffer.of(env.colors(), env.stderr())) {
                buffer.print("Summary", ColorTheme.EM_UNDERLINE);
                buffer.println(String.format(": Smithy built %s projection(s), %s plugin(s), and %s artifacts",
                                       resultConsumer.projectionCount,
                                       resultConsumer.pluginCount,
                                       resultConsumer.artifactCount));
            }
        }

        // Throw an exception if any errors occurred.
        if (!resultConsumer.failedProjections.isEmpty()) {
            resultConsumer.failedProjections.sort(String::compareTo);
            StringBuilder error = new StringBuilder();
            try (ColorBuffer buffer = ColorBuffer.of(env.colors(), error)) {
                buffer.println();
                buffer.println(String.format(
                        "The following %d Smithy build projection(s) failed: %s",
                        resultConsumer.failedProjections.size(),
                        resultConsumer.failedProjections));
            }
            throw new CliError(error.toString());
        }

        return 0;
    }

    private static final class ResultConsumer implements Consumer<ProjectionResult>, BiConsumer<String, Throwable> {
        private final List<String> failedProjections = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger artifactCount = new AtomicInteger();
        private final AtomicInteger pluginCount = new AtomicInteger();
        private final AtomicInteger projectionCount = new AtomicInteger();
        private final boolean quiet;
        private final ColorFormatter colors;
        private final CliPrinter printer;

        ResultConsumer(ColorFormatter colors, CliPrinter stderr, boolean quiet) {
            this.colors = colors;
            this.printer = stderr;
            this.quiet = quiet;
        }

        @Override
        public void accept(String name, Throwable exception) {
            failedProjections.add(name);
            StringWriter writer = new StringWriter();
            writer.write(String.format("%nProjection %s failed: %s%n", name, exception.toString()));
            exception.printStackTrace(new PrintWriter(writer));
            colors.println(printer, writer.toString(), ColorTheme.ERROR);
        }

        @Override
        public void accept(ProjectionResult result) {
            try (ColorBuffer buffer = ColorBuffer.of(colors, printer)) {
                String status;
                Style statusStyle;

                if (result.isBroken()) {
                    failedProjections.add(result.getProjectionName());
                    statusStyle = ColorTheme.ERROR;
                    status = "Failed";
                } else {
                    // Only increment the projection count if it succeeded.
                    projectionCount.incrementAndGet();
                    statusStyle = ColorTheme.SUCCESS;
                    status = "Completed";
                }

                pluginCount.addAndGet(result.getPluginManifests().size());

                // Increment the total number of artifacts written.
                for (FileManifest manifest : result.getPluginManifests().values()) {
                    artifactCount.addAndGet(manifest.getFiles().size());
                }

                // Get the base directory of the projection.
                Iterator<FileManifest> manifestIterator = result.getPluginManifests().values().iterator();
                Path root = manifestIterator.hasNext() ? manifestIterator.next().getBaseDir().getParent() : null;

                if (!quiet) {
                    int remainingLength = 80 - 6 - result.getProjectionName().length();
                    buffer.style(w -> {
                        w.append("──  ");
                        w.append(result.getProjectionName());
                        w.append("  ");
                        for (int i = 0; i < remainingLength; i++) {
                            w.append("─");
                        }
                        w.println();
                    }, statusStyle);
                    buffer
                            .print(status)
                            .append(" projection ")
                            .append(result.getProjectionName())
                            .append(" (")
                            .append(String.valueOf(result.getModel().toSet().size()))
                            .append("): ")
                            .append(String.valueOf(root))
                            .println();
                }

                if (result.isBroken()) {
                    SourceContextLoader loader = SourceContextLoader.createModelAwareLoader(result.getModel(), 4);
                    PrettyAnsiValidationFormatter formatter = PrettyAnsiValidationFormatter.builder()
                            .sourceContextLoader(loader)
                            .colors(colors)
                            .titleLabel(result.getProjectionName(), statusStyle)
                            .build();
                    result.getEvents().forEach(event -> {
                        if (event.getSeverity() == Severity.DANGER || event.getSeverity() == Severity.ERROR) {
                            buffer.println(formatter.format(event));
                        }
                    });
                }

                buffer.println();
            }
        }
    }
}

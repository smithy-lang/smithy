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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.ProjectionResult;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class BuildCommand extends SimpleCommand {
    private static final Logger LOGGER = Logger.getLogger(BuildCommand.class.getName());

    public BuildCommand(String parentCommandName) {
        super(parentCommandName);
    }

    @Override
    public String getName() {
        return "build";
    }

    @Override
    public String getSummary() {
        return "Builds Smithy models and creates plugin artifacts for each projection found in smithy-build.json.";
    }

    private static final class Options implements ArgumentReceiver {
        private final List<String> config = new ArrayList<>();
        private String output;
        private String projection;
        private String plugin;

        @Override
        public boolean testOption(String name) {
            return false;
        }

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--config":
                case "-c":
                    return config::add;
                case "--output":
                    return value -> output = value;
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
            printer.param("--config", "-c", "CONFIG_PATH...",
                          "Path to smithy-build.json configuration (defaults to './smithy-build.json'). This option "
                          + "can be repeated and each configured will be merged.");
            printer.param("--projection", null, "PROJECTION_NAME", "Only generate artifacts for this projection.");
            printer.param("--plugin", null, "PLUGIN_NAME", "Only generate artifacts for this plugin.");
            printer.param("--output", null, "OUTPUT_PATH",
                          "Where to write artifacts (defaults to './build/smithy').");
        }
    }

    @Override
    protected List<ArgumentReceiver> createArgumentReceivers() {
        return ListUtils.of(new BuildOptions(), new Options());
    }

    @Override
    protected int run(Arguments arguments, Env env, List<String> models) {
        Options options = arguments.getReceiver(Options.class);
        StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);
        String output = options.output;

        LOGGER.fine(() -> String.format("Building Smithy model sources: %s", models));
        SmithyBuildConfig.Builder configBuilder = SmithyBuildConfig.builder();
        List<String> config = getConfig(options);

        if (!config.isEmpty()) {
            LOGGER.fine(() -> String.format("Loading Smithy configs: [%s]", String.join(" ", config)));
            config.forEach(file -> configBuilder.load(Paths.get(file)));
        } else {
            configBuilder.version(SmithyBuild.VERSION);
        }

        if (output != null) {
            configBuilder.outputDirectory(output);
            try {
                Files.createDirectories(Paths.get(output));
                LOGGER.info(() -> "Output directory set to: " + output);
            } catch (IOException e) {
                throw new CliError("Unable to create Smithy output directory: " + e.getMessage());
            }
        }

        SmithyBuildConfig smithyBuildConfig = configBuilder.build();

        // Build the model and fail if there are errors. Prints errors to stdout.
        // Configure whether the build is quiet or not based on the --quiet option.
        Model model = CommandUtils.buildModel(arguments, models, env, env.stderr(), standardOptions.quiet());

        SmithyBuild smithyBuild = SmithyBuild.create(env.classLoader())
                .config(smithyBuildConfig)
                .model(model);

        if (options.plugin != null) {
            smithyBuild.pluginFilter(name -> name.equals(options.plugin));
        }

        if (options.projection != null) {
            smithyBuild.projectionFilter(name -> name.equals(options.projection));
        }

        // Register sources with the builder.
        models.forEach(path -> smithyBuild.registerSources(Paths.get(path)));

        ResultConsumer resultConsumer = new ResultConsumer(env.stderr(), standardOptions.quiet());
        smithyBuild.build(resultConsumer, resultConsumer);

        if (!standardOptions.quiet()) {
            Style ansiColor = resultConsumer.failedProjections.isEmpty()
                              ? Style.BRIGHT_GREEN
                              : Style.BRIGHT_YELLOW;
            env.stderr().println(env.stderr().style(
                    String.format("Smithy built %s projection(s), %s plugin(s), and %s artifacts",
                                  resultConsumer.projectionCount,
                                  resultConsumer.pluginCount,
                                  resultConsumer.artifactCount),
                    Style.BOLD, ansiColor));
        }

        // Throw an exception if any errors occurred.
        if (!resultConsumer.failedProjections.isEmpty()) {
            resultConsumer.failedProjections.sort(String::compareTo);
            throw new CliError(String.format(
                    "The following %d Smithy build projection(s) failed: %s",
                    resultConsumer.failedProjections.size(),
                    resultConsumer.failedProjections));
        }

        return 0;
    }

    private List<String> getConfig(Options options) {
        List<String> config = options.config;
        if (config.isEmpty() && Files.exists(Paths.get("smithy-build.json"))) {
            config = Collections.singletonList("smithy-build.json");
        }
        return config;
    }

    private static final class ResultConsumer implements Consumer<ProjectionResult>, BiConsumer<String, Throwable> {
        private final List<String> failedProjections = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger artifactCount = new AtomicInteger();
        private final AtomicInteger pluginCount = new AtomicInteger();
        private final AtomicInteger projectionCount = new AtomicInteger();
        private final boolean quiet;
        private final CliPrinter printer;

        ResultConsumer(CliPrinter stderr, boolean quiet) {
            this.printer = stderr;
            this.quiet = quiet;
        }

        @Override
        public void accept(String name, Throwable exception) {
            failedProjections.add(name);
            StringBuilder message = new StringBuilder(
                    String.format("%nProjection %s failed: %s%n", name, exception.toString()));

            for (StackTraceElement element : exception.getStackTrace()) {
                message.append(element).append(System.lineSeparator());
            }

            // Always print errors.
            printer.println(printer.style(message.toString(), Style.RED));
        }

        @Override
        public void accept(ProjectionResult result) {
            if (result.isBroken()) {
                // Write out validation errors as they occur.
                failedProjections.add(result.getProjectionName());
                StringBuilder message = new StringBuilder(System.lineSeparator());
                message.append(result.getProjectionName())
                        .append(" has a model that failed validation")
                        .append(System.lineSeparator());
                result.getEvents().forEach(event -> {
                    if (event.getSeverity() == Severity.DANGER || event.getSeverity() == Severity.ERROR) {
                        message.append(event).append(System.lineSeparator());
                    }
                });
                // Always print errors.
                printer.println(printer.style(message.toString(), Style.RED));
            } else {
                // Only increment the projection count if it succeeded.
                projectionCount.incrementAndGet();
            }

            pluginCount.addAndGet(result.getPluginManifests().size());

            // Get the base directory of the projection.
            Iterator<FileManifest> manifestIterator = result.getPluginManifests().values().iterator();
            Path root = manifestIterator.hasNext() ? manifestIterator.next().getBaseDir().getParent() : null;

            if (!quiet) {
                String message = String.format("Completed projection %s (%d shapes): %s",
                                               result.getProjectionName(), result.getModel().toSet().size(), root);
                printer.println(printer.style(message, Style.GREEN));
            }

            // Increment the total number of artifacts written.
            for (FileManifest manifest : result.getPluginManifests().values()) {
                artifactCount.addAndGet(manifest.getFiles().size());
            }
        }
    }
}

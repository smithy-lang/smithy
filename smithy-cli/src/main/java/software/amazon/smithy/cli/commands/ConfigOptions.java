/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.HelpPrinter;

final class ConfigOptions implements ArgumentReceiver {

    private static final Logger LOGGER = Logger.getLogger(ConfigOptions.class.getName());
    private final List<String> config = new ArrayList<>();
    private boolean noConfig = false;
    private Path root;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.param("--config",
                "-c",
                "CONFIG_PATH...",
                "Path to smithy-build.json config (defaults to ./smithy-build.json if not specified). "
                        + "This option can be repeated, merging each config file.");
        printer.option("--no-config", null, "Disable config file detection and use.");
    }

    @Override
    public Consumer<String> testParameter(String name) {
        switch (name) {
            case "--config":
            case "-c":
                return config::add;
            default:
                return null;
        }
    }

    @Override
    public boolean testOption(String name) {
        if (name.equals("--no-config")) {
            noConfig = true;
            return true;
        } else {
            return false;
        }
    }

    void root(Path root) {
        this.root = root;
    }

    List<String> config() {
        List<String> config = this.config;

        // Don't find the default config if --no-config is passed.
        if (config.isEmpty() && !noConfig) {
            Path defaultConfig = root != null
                    ? root.resolve("smithy-build.json").toAbsolutePath()
                    : Paths.get("smithy-build.json").toAbsolutePath();
            if (Files.exists(defaultConfig)) {
                LOGGER.fine("Detected smithy-build.json at " + defaultConfig);
                config = Collections.singletonList(defaultConfig.toString());
            }
        }
        return config;
    }

    SmithyBuildConfig createSmithyBuildConfig() {
        long startTime = System.nanoTime();
        SmithyBuildConfig smithyBuildConfig;
        List<String> config = config();

        if (noConfig && !config.isEmpty()) {
            throw new CliError("Invalid combination of --no-config and --config. --no-config can be omitted because "
                    + "providing --config/-c disables automatically loading ./smithy-build.json.");
        }

        if (config.isEmpty()) {
            smithyBuildConfig = SmithyBuildConfig.builder().version(SmithyBuild.VERSION).build();
        } else {
            LOGGER.fine(() -> String.format("Loading Smithy configs: [%s]", String.join(" ", config)));
            SmithyBuildConfig.Builder configBuilder = SmithyBuildConfig.builder();
            // Set the lastModified time in millis of the builder to the latest modified date of any config.
            long newestLastModified = 0;
            for (String configFile : config) {
                File file = new File(configFile);
                newestLastModified = Math.max(newestLastModified, file.lastModified());
                configBuilder.load(file.toPath());
            }
            configBuilder.lastModifiedInMillis(newestLastModified);
            smithyBuildConfig = configBuilder.build();
        }

        LOGGER.fine(() -> "Smithy config load time in ms: " + ((System.nanoTime() - startTime) / 1000000));
        return smithyBuildConfig;
    }
}

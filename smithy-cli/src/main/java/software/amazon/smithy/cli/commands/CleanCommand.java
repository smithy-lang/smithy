/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.utils.IoUtils;

final class CleanCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(CleanCommand.class.getName());
    private final String parentCommandName;

    CleanCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
    }

    @Override
    public String getName() {
        return "clean";
    }

    @Override
    public String getSummary() {
        return "Removes Smithy build artifacts and caches.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new Options());

        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, this::run);
        return action.apply(arguments, env);
    }

    private int run(Arguments arguments, Env env) {
        ConfigOptions configOptions = arguments.getReceiver(ConfigOptions.class);
        Options options = arguments.getReceiver(Options.class);

        if (options.cleanTemplateCache) {
            LOGGER.fine(() -> "Clearing template cache.");
            if (CliCache.getTemplateCache().clear()) {
                LOGGER.fine(() -> "No template cache found.");
            }
            return 0;
        }

        SmithyBuildConfig config = configOptions.createSmithyBuildConfig();
        Path dir = config.getOutputDirectory()
                .map(Paths::get)
                .orElseGet(SmithyBuild::getDefaultOutputDirectory);
        LOGGER.fine(() -> "Deleting directory: " + dir);
        if (!IoUtils.rmdir(dir)) {
            LOGGER.fine(() -> "Directory does not exist: " + dir);
        }
        LOGGER.fine(() -> "Deleted directory " + dir);

        LOGGER.fine(() -> "Clearing all caches.");
        if (!CliCache.clearAll()) {
            LOGGER.fine(() -> "No caches found.");
        }

        return 0;
    }

    private static final class Options implements ArgumentReceiver {
        private Boolean cleanTemplateCache = false;

        @Override
        public boolean testOption(String name) {
            switch (name) {
                case "--templates":
                case "-t":
                    cleanTemplateCache = true;
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--templates",
                    "-t",
                    null,
                    "Clean only the templates cache.");
        }
    }
}

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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;
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
        return "Removes Smithy build artifacts.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());

        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, this::run);
        return action.apply(arguments, env);
    }

    private int run(Arguments arguments, Env env) {
        ConfigOptions options = arguments.getReceiver(ConfigOptions.class);
        SmithyBuildConfig config = options.createSmithyBuildConfig();
        Path dir = config.getOutputDirectory()
                .map(Paths::get)
                .orElseGet(SmithyBuild::getDefaultOutputDirectory);
        LOGGER.fine(() -> "Deleting directory: " + dir);
        if (!IoUtils.rmdir(dir)) {
            LOGGER.fine(() -> "Directory does not exist: " + dir);
        }
        LOGGER.fine(() -> "Deleted directory " + dir);
        return 0;
    }
}

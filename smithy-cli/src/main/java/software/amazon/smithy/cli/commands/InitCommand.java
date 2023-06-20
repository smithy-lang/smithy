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

import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InitCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(InitCommand.class.getName());

    private final String parentCommandName;

    public InitCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
    }

    @Override
    public String getName() {
        return "init";
    }

    @Override
    public String getSummary() {
        return "Init a smithy workspace by template";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new Options());
        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, this::run);
        return action.apply(arguments, env);
    }

    public void executeScript(String repoUrl, String template) throws IOException, InterruptedException {
        String command = String.format("sh /Volumes/workplace/smithy/smithy-cli/clone-template.sh %s %s", repoUrl, template);
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();

        BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        String line = "";
        while ((line = errorReader.readLine()) != null) {
            LOGGER.log(Level.INFO, line);
        }
    }

    private int run(Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);
        try {
            this.executeScript(options.repoUrl, options.template);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    private static final class Options implements ArgumentReceiver {
        private String template;

        private String repoUrl = "ssh://git.amazon.com/pkg/Smithy-Examples";

        @Override
        public boolean testOption(String name) {
            return false;
        }

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--template":
                    return value -> template = value;
                case "--url":
                    return value -> repoUrl = value;
                default:
                    return value -> {
                    };
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--template", null, "",
                    "template name");
            printer.param("--url", null, "",
                    "repo url");
        }
    }
}

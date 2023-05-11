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

import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.HelpPrinter;

public class ActionEnvOptions implements ArgumentReceiver {
    private static final Logger LOGGER = Logger.getLogger(ActionEnvOptions.class.getName());

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.param("--env", "-e", "MY_VAR=myValue...",
            "Environment variable to use for commmand execution. Note: overrides local env.");
    }

    @Override
    public Consumer<String> testParameter(String name) {
        switch (name) {
            case "--env":
            case "-e":
                return this::addToProperties;
            default:
                return null;
        }
    }

    private void addToProperties(String input) {
         String[] values = input.split("=");
         if (values.length != 2) {
             LOGGER.warning("Environment variable incorrect. Expected Key value pair of form "
                 + "KEY=VALUE, but found `" + input + "`");
             return;
         }
         LOGGER.info("Adding environment variable to action environment: " + input);
         System.setProperty(values[0], values[1]);
    }
}

/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.upgrade;

import java.io.IOException;
import java.util.logging.Logger;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Cli;
import software.amazon.smithy.cli.Colors;
import software.amazon.smithy.cli.Parser;
import software.amazon.smithy.cli.SmithyCli;

public final class SmithyUpgradeCli  {
    private static final Logger LOGGER = Logger.getLogger(SmithyUpgradeCli.class.getName());
    private static final Parser PARSER = Parser.builder()
            .parameter("--config", "-c",
                    "Path to smithy-build.json configuration. Defaults to 'smithy-build.json'")
            .parameter(SmithyCli.DISCOVER_CLASSPATH, "Enables model discovery using a custom classpath for models")
            .positional("<MODELS>", "Path to Smithy models or directories")
            .build();


    private SmithyUpgradeCli() {}

    public static void main(String... args) {
        new SmithyUpgradeCli().handle(args);
    }

    private void handle(String... args) {
        Arguments arguments = PARSER.parse(args);
        if (arguments.has(Cli.HELP)) {
            printHelp();
            return;
        }
        try {
            Upgrader.upgradeFiles(
                    arguments.positionalArguments(),
                    arguments.parameter(SmithyCli.DISCOVER_CLASSPATH, null),
                    arguments.parameter("--config")
            );
        } catch (IOException ignored) {
        }
    }

    private void printHelp() {
        StringBuilder usageBuilder = new StringBuilder("Usage: smithy-upgrade ");

        StringBuilder paramBuilder = new StringBuilder();
        if (PARSER.getPositionalName().isPresent()) {
            usageBuilder.append(PARSER.getPositionalName().get());
            paramBuilder.append("    ")
                    .append(PARSER.getPositionalName().get())
                    .append(": ")
                    .append(PARSER.getPositionalHelp().get())
                    .append("\n\n");
        }

        for (Parser.Argument arg : PARSER.getArgs()) {
            paramBuilder.append("    ")
                    .append(arg.getCanonicalName())
                    .append(": ")
                    .append(arg.getHelp())
                    .append("\n\n");

            String arityValue = " ...";
            if (arg.getArity().equals(Parser.Arity.NONE)) {
                arityValue = "";
            }

            usageBuilder.append(" [ ");
            if (arg.getShortName().isPresent() && arg.getLongName().isPresent()) {
                usageBuilder
                        .append(arg.getShortName().get())
                        .append(arityValue)
                        .append(" | ")
                        .append(arg.getLongName().get())
                        .append(arityValue)
                        .append(" ]");
            } else {
                usageBuilder.append(arg.getCanonicalName()).append(arityValue).append(" ]");
            }
        }

        usageBuilder.append("\n\n").append(paramBuilder);

        Colors.BRIGHT_WHITE.out(usageBuilder.toString());
    }
}

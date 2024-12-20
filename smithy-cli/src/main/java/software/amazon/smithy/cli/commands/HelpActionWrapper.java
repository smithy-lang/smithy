/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.utils.StringUtils;

/**
 * A {@code CommandAction} that intercepts another action and handles printing -h / --help.
 */
final class HelpActionWrapper implements CommandAction {

    private static final Logger LOGGER = Logger.getLogger(HelpActionWrapper.class.getName());
    private final String name;
    private final String parentCommandName;
    private final Function<ColorFormatter, String> documentationProvider;
    private final CommandAction delegate;
    private final String summary;

    HelpActionWrapper(
            String name,
            String parentCommandName,
            String summary,
            Function<ColorFormatter, String> documentationProvider,
            CommandAction delegate
    ) {
        this.name = name;
        this.parentCommandName = parentCommandName;
        this.summary = summary;
        this.documentationProvider = documentationProvider;
        this.delegate = delegate;
    }

    static HelpActionWrapper fromCommand(Command command, String parentCommandName, CommandAction delegate) {
        return fromCommand(command, parentCommandName, colors -> "", delegate);
    }

    static HelpActionWrapper fromCommand(
            Command command,
            String parentCommandName,
            Function<ColorFormatter, String> documentationProvider,
            CommandAction delegate
    ) {
        return new HelpActionWrapper(
                command.getName(),
                parentCommandName,
                command.getSummary(),
                documentationProvider,
                delegate);
    }

    @Override
    public int apply(Arguments arguments, Command.Env env) {
        // Force parsing to finish.
        List<String> positionalArguments = arguments.getPositional();

        if (arguments.getReceiver(StandardOptions.class).help()) {
            printHelp(arguments, env.colors(), env.stdout());
            return 0;
        }

        LOGGER.fine(() -> "Invoking Command with positional arguments: " + positionalArguments);
        return delegate.apply(arguments, env);
    }

    private void printHelp(Arguments arguments, ColorFormatter colors, CliPrinter printer) {
        String title = StringUtils.isEmpty(parentCommandName) ? name : parentCommandName + " " + name;
        HelpPrinter.fromArguments(title, arguments)
                .summary(summary)
                .documentation(documentationProvider.apply(colors))
                .print(colors, printer);
    }
}

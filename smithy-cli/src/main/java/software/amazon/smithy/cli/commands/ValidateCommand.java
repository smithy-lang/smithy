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

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class ValidateCommand extends CommandWithHelp {

    private static final Logger LOGGER = Logger.getLogger(ValidateCommand.class.getName());

    @Override
    public String getName() {
        return "validate";
    }

    @Override
    public String getSummary() {
        return "Validates Smithy models";
    }

    @Override
    public void printHelp(CliPrinter printer) {
        printer.println("Usage: smithy validate [-h | --help] [--allow-unknown-traits] [-d | --discover]");
        printer.println("                       [--discover-classpath JAVA_CLASSPATH]");
        printer.println("                       [--severity SEVERITY] [--debug] [--quiet] [--stacktrace]");
        printer.println("                       [--no-color] [--force-color] [--logging LOG_LEVEL]");
        printer.println("                       <MODELS>...");
        printer.println("");
        printer.println(getSummary());
        printer.println("");
        StandardOptions.printHelp(printer);
        BuildOptions.printHelp(printer);
        printer.println(printer.style("    <MODELS>...", Style.YELLOW));
        printer.println("        Path to Smithy models or directories to load.");
    }

    @Override
    protected List<String> parseArguments(Arguments arguments, Env env) {
        arguments.addReceiver(new BuildOptions());
        return arguments.finishParsing();
    }

    @Override
    protected int run(Arguments arguments, Env env, List<String> models) {
        StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);
        LOGGER.info(() -> "Validating Smithy model sources: " + models);
        CommandUtils.buildModel(arguments, models, env, env.stdout(), standardOptions.quiet());
        LOGGER.info("Smithy validation complete");
        return 0;
    }
}

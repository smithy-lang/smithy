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

package software.amazon.smithy.cli;

import java.util.function.Consumer;
import java.util.logging.Level;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Options available to all commands.
 */
@SmithyInternalApi
public final class StandardOptions implements ArgumentReceiver {

    private boolean help;
    private Severity severity = Severity.WARNING;
    private Level logging = Level.WARNING;
    private boolean quiet;
    private boolean debug;
    private boolean stackTrace;
    private boolean noColor;
    private boolean forceColor;

    public static void printHelp(CliPrinter printer) {
        printer.println(printer.style("    --help, -h", Style.YELLOW));
        printer.println("        Prints this help information.");
        printer.println(printer.style("    --debug", Style.YELLOW));
        printer.println("        Display debug information");
        printer.println(printer.style("    --quiet", Style.YELLOW));
        printer.println("        Silences all output except errors.");
        printer.println(printer.style("    --stacktrace", Style.YELLOW));
        printer.println("        Display a stacktrace on error");
        printer.println(printer.style("    --no-color", Style.YELLOW));
        printer.println("        Explicitly disable ANSI colors");
        printer.println(printer.style("    --force-color", Style.YELLOW));
        printer.println("        Explicitly enable ANSI colors");
        printer.println(printer.style("    --logging <LOG_LEVEL>", Style.YELLOW));
        printer.println("        Sets the log level to one of OFF, SEVERE, WARNING (default), INFO, FINE, ALL");
        printer.println(printer.style("    --severity <SEVERITY>", Style.YELLOW));
        printer.println("        Sets the minimum reported validation severity to report. Set to one of");
        printer.println("        NOTE, WARNING (default), DANGER, ERROR");
    }

    @Override
    public boolean testOption(String name) {
        switch (name) {
            case "--help":
            case "-h":
                help = true;
                return true;
            case "--debug":
                debug = true;
                quiet = false;
                // Automatically set logging level to ALL.
                logging = Level.ALL;
                return true;
            case "--quiet":
                quiet = true;
                debug = false;
                // Automatically set logging level to SEVERE.
                logging = Level.SEVERE;
                // Automatically set severity to DANGER.
                severity = Severity.DANGER;
                return true;
            case "--stacktrace":
                stackTrace = true;
                return true;
            case "--no-color":
                noColor = true;
                forceColor = false;
                return true;
            case "--force-color":
                noColor = false;
                forceColor = true;
                return true;
            default:
                return false;
        }
    }

    @Override
    public Consumer<String> testParameter(String name) {
        switch (name) {
            case "--logging":
                return value -> {
                    try {
                        logging = Level.parse(value);
                    } catch (IllegalArgumentException e) {
                        throw new CliError("Invalid logging level: " + value);
                    }
                };
            case "--severity":
                return value -> {
                    severity = Severity.fromString(value).orElseThrow(() -> {
                        return new CliError("Invalid severity level: " + value);
                    });
                };
            default:
                return null;
        }
    }

    public boolean help() {
        return help;
    }

    public Severity severity() {
        return severity;
    }

    public Level logging() {
        return logging;
    }

    public boolean quiet() {
        return quiet;
    }

    public boolean debug() {
        return debug;
    }

    public boolean stackTrace() {
        return stackTrace;
    }

    public boolean noColor() {
        return noColor;
    }

    public boolean forceColor() {
        return forceColor;
    }
}

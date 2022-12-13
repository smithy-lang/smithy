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

    public static final String HELP_SHORT = "-h";
    public static final String HELP = "--help";
    public static final String DEBUG = "--debug";
    public static final String QUIET = "--quiet";
    public static final String STACKTRACE = "--stacktrace";
    public static final String NO_COLOR = "--no-color";
    public static final String FORCE_COLOR = "--force-color";
    public static final String LOGGING = "--logging";
    public static final String SEVERITY = "--severity";

    private boolean help;
    private Severity severity = Severity.WARNING;
    private Level logging = Level.WARNING;
    private boolean quiet;
    private boolean debug;
    private boolean stackTrace;
    private boolean noColor;
    private boolean forceColor;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.option(HELP, HELP_SHORT, "Prints this help output");
        printer.option(DEBUG, null, "Display debug information");
        printer.option(QUIET, null, "Silences all output except errors");
        printer.option(STACKTRACE, null, "Display a stacktrace on error");
        printer.option(NO_COLOR, null, "Explicitly disable ANSI colors");
        printer.option(FORCE_COLOR, null, "Explicitly enable ANSI colors");
        printer.param(LOGGING, null, "LOG_LEVEL",
                            "Sets the log level (defaults to WARNING). Set to one of OFF, SEVERE, WARNING, INFO, "
                            + "FINE, ALL.");
        printer.param(SEVERITY, null, "SEVERITY", "Sets the minimum reported validation severity to "
                                                      + "report. Set to one of NOTE, WARNING (default), "
                                                      + "DANGER, ERROR");
    }

    @Override
    public boolean testOption(String name) {
        switch (name) {
            case HELP:
            case HELP_SHORT:
                help = true;
                return true;
            case DEBUG:
                debug = true;
                quiet = false;
                // Automatically set logging level to ALL.
                logging = Level.ALL;
                return true;
            case QUIET:
                quiet = true;
                debug = false;
                // Automatically set logging level to SEVERE.
                logging = Level.SEVERE;
                // Automatically set severity to DANGER.
                severity = Severity.DANGER;
                return true;
            case STACKTRACE:
                stackTrace = true;
                return true;
            case NO_COLOR:
                noColor = true;
                forceColor = false;
                return true;
            case FORCE_COLOR:
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
            case LOGGING:
                return value -> {
                    try {
                        logging = Level.parse(value);
                    } catch (IllegalArgumentException e) {
                        throw new CliError("Invalid logging level: " + value);
                    }
                };
            case SEVERITY:
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

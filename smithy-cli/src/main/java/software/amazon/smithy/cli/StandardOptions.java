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
import java.util.logging.Logger;
import software.amazon.smithy.model.validation.Severity;

/**
 * Options available to all commands.
 */
public final class StandardOptions implements ArgumentReceiver {

    public static final String HELP_SHORT = "-h";
    public static final String HELP = "--help";
    public static final String VERSION = "--version";
    public static final String DEBUG = "--debug";
    public static final String QUIET = "--quiet";
    public static final String STACKTRACE = "--stacktrace";
    public static final String LOGGING = "--logging";
    public static final String SEVERITY = "--severity";

    private static final Logger LOGGER = Logger.getLogger(StandardOptions.class.getName());

    private boolean help;
    private boolean version;
    private Severity severity = Severity.WARNING;
    private Level logging = Level.WARNING;
    private boolean quiet;
    private boolean debug;
    private boolean stackTrace;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.option(HELP, HELP_SHORT, "Print help output");
        printer.option(DEBUG, null, "Display debug information");
        printer.option(QUIET, null, "Silence output except errors");
        printer.option(STACKTRACE, null, "Display a stacktrace on error");
        printer.param(LOGGING, null, "LOG_LEVEL",
                            "Set the log level (defaults to WARNING). Set to one of OFF, SEVERE, WARNING, INFO, "
                            + "FINE, ALL.");
        printer.param(SEVERITY, null, "SEVERITY", "Set the minimum reported validation severity (one of NOTE, "
                                                  + "WARNING [default setting], DANGER, ERROR).");
    }

    @Override
    public boolean testOption(String name) {
        switch (name) {
            case HELP:
            case HELP_SHORT:
                help = true;
                return true;
            case VERSION:
                version = true;
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
            case "--no-color":
                LOGGER.warning("--no-color is no longer supported. Use the NO_COLOR environment variable.");
                return true;
            case "--force-color":
                LOGGER.warning("--force-color is no longer supported. Use the FORCE_COLOR environment variable.");
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

    public boolean version() {
        return version;
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
}

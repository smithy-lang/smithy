/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Options available to all commands.
 */
public final class StandardOptions implements ArgumentReceiver {

    public static final String HELP_SHORT = "-h";
    public static final String HELP = "--help";
    public static final String DEBUG = "--debug";
    public static final String QUIET = "--quiet";
    public static final String STACKTRACE = "--stacktrace";
    public static final String NO_COLOR = "--no-color";
    public static final String FORCE_COLOR = "--force-color";
    public static final String LOGGING = "--logging";

    private boolean help;
    private Level logging = Level.WARNING;
    private boolean quiet;
    private boolean debug;
    private boolean stackTrace;
    private AnsiColorFormatter colorSetting = AnsiColorFormatter.AUTO;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.option(HELP, HELP_SHORT, "Print this help output.");
        printer.option(DEBUG, null, "Display debug information.");
        printer.option(QUIET, null, "Silence output except errors.");
        printer.option(NO_COLOR, null, "Disable ANSI colors.");
        printer.option(FORCE_COLOR, null, "Force the use of ANSI colors.");
        printer.option(STACKTRACE, null, "Display a stacktrace on error.");
        printer.param(LOGGING,
                null,
                "LOG_LEVEL",
                "Set the log level (defaults to WARNING). Set to one of OFF, SEVERE, WARNING, INFO, "
                        + "FINE, ALL.");
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
                return true;
            case STACKTRACE:
                stackTrace = true;
                return true;
            case NO_COLOR:
                colorSetting = AnsiColorFormatter.NO_COLOR;
                return true;
            case FORCE_COLOR:
                colorSetting = AnsiColorFormatter.FORCE_COLOR;
                return true;
            default:
                return false;
        }
    }

    @Override
    public Consumer<String> testParameter(String name) {
        if (LOGGING.equals(name)) {
            return value -> {
                try {
                    logging = Level.parse(value);
                } catch (IllegalArgumentException e) {
                    throw new CliError("Invalid logging level: " + value);
                }
            };
        }
        return null;
    }

    public boolean help() {
        return help;
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

    public AnsiColorFormatter colorSetting() {
        return colorSetting;
    }
}

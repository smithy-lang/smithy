/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Package-private utilities for configuring CLI logging.
 */
final class LoggingUtil {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final List<Runnable> RESTORE_FUNCTIONS = new ArrayList<>();

    private LoggingUtil() {}

    static void configureLogging(StandardOptions options, CliPrinter printer) {
        // Don't try to configure logging more than once.
        if (!RESTORE_FUNCTIONS.isEmpty()) {
            return;
        }

        Level level = options.logging();
        Logger rootLogger = Logger.getLogger("");

        // Set the root level, but try to restore it later.
        Level previousRootLevel = rootLogger.getLevel();
        RESTORE_FUNCTIONS.add(() -> rootLogger.setLevel(previousRootLevel));
        rootLogger.setLevel(level);

        for (Handler h : rootLogger.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                // Remove any console log handlers.
                rootLogger.removeHandler(h);
                RESTORE_FUNCTIONS.add(() -> rootLogger.addHandler(h));
            } else if (h.getLevel() != level) {
                // Change the log level if needed.
                Level currentLevel = h.getLevel();
                RESTORE_FUNCTIONS.add(() -> h.setLevel(currentLevel));
                h.setLevel(level);
            }
        }

        // Add the CLI's custom CLI handler to output CLI-friendly messages to stderr.
        addCliHandler(options.debug(), level, rootLogger, printer);
    }

    static void restoreLogging() {
        for (Runnable runnable : RESTORE_FUNCTIONS) {
            runnable.run();
        }
        RESTORE_FUNCTIONS.clear();
    }

    private static void addCliHandler(boolean debug, Level level, Logger rootLogger, CliPrinter printer) {
        if (level != Level.OFF) {
            Handler handler = debug
                    // Debug ignores the given logging level and just logs everything.
                    ? new CliLogHandler(new DebugFormatter(), printer)
                    : new CliLogHandler(new BasicFormatter(), printer);
            handler.setLevel(level);
            rootLogger.addHandler(handler);
            RESTORE_FUNCTIONS.add(() -> rootLogger.removeHandler(handler));
        }
    }

    private static final class BasicFormatter extends SimpleFormatter {
        @Override
        public synchronized String format(LogRecord r) {
            return '[' + r.getLevel().getLocalizedName() + "] " + r.getMessage();
        }
    }

    private static final class DebugFormatter extends SimpleFormatter {
        @Override
        public synchronized String format(LogRecord r) {
            StringBuilder result = new StringBuilder();
            result.append(FORMAT.format(new Date(r.getMillis())));
            result.append(' ');
            result.append('[');
            result.append(Thread.currentThread().getName());
            result.append(']');
            result.append(' ');
            result.append(r.getLevel().getLocalizedName());
            result.append(' ');
            result.append(r.getLoggerName());
            result.append(' ');
            result.append('-');
            result.append(' ');
            result.append(r.getMessage());
            return result.toString();
        }
    }

    /**
     * Logs messages to the CLI's redirect stderr.
     */
    private static final class CliLogHandler extends Handler {
        private final Formatter formatter;
        private final CliPrinter printer;

        CliLogHandler(Formatter formatter, CliPrinter printer) {
            this.formatter = formatter;
            this.printer = printer;
        }

        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                String formatted = formatter.format(record);
                if (record.getLevel().equals(Level.SEVERE)) {
                    printer.println(printer.style(formatted, Style.RED));
                } else if (record.getLevel().equals(Level.WARNING)) {
                    printer.println(printer.style(formatted, Style.YELLOW));
                } else {
                    printer.println(formatted);
                }
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}

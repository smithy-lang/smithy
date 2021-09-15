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
import java.util.Date;
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

    private LoggingUtil() {}

    static void configureLogging(boolean debug, Level level) {
        Logger rootLogger = Logger.getLogger("");
        removeConsoleHandler(rootLogger);
        addCliHandler(debug, level, rootLogger);
    }

    private static void addCliHandler(boolean debug, Level level, Logger rootLogger) {
        if (level != Level.OFF) {
            Handler handler = debug
                    // Debug ignores the given logging level and just logs everything.
                    ? new CliLogHandler(new DebugFormatter())
                    : new CliLogHandler(new BasicFormatter());
            handler.setLevel(level);
            rootLogger.addHandler(handler);
        }

        rootLogger.setLevel(level);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(level);
        }
    }

    private static void removeConsoleHandler(Logger rootLogger) {
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                // Remove any console log handlers.
                rootLogger.removeHandler(handler);
            }
        }
    }

    private static final class BasicFormatter extends SimpleFormatter {
        @Override
        public synchronized String format(LogRecord r) {
            return FORMAT.format(new Date(r.getMillis()))
                   + " " + r.getLevel().getLocalizedName() + " - "
                   + r.getMessage();
        }
    }

    private static final class DebugFormatter extends SimpleFormatter {
        @Override
        public synchronized String format(LogRecord r) {
            return FORMAT.format(new Date(r.getMillis()))
                   + " [" + Thread.currentThread().getName() + "] "
                   + r.getLevel().getLocalizedName() + " "
                   + r.getLoggerName() + " - "
                   + r.getMessage();
        }
    }

    /**
     * Logs messages to the CLI's redirect stderr.
     */
    private static final class CliLogHandler extends Handler {
        private final Formatter formatter;

        CliLogHandler(Formatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                Cli.stderr(formatter.format(record));
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

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
 * Wraps {@code Arguments} and configures logging based on arguments that aren't yet parsed.
 */
final class LoggingArgumentsHandler implements Arguments {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private final Arguments delegate;
    private boolean configuredLogging;
    private final List<Runnable> restoreFunctions = new ArrayList<>();
    private final Logger rootLogger = Logger.getLogger("");
    private final Handler smithyHandler;

    LoggingArgumentsHandler(ColorFormatter colors, CliPrinter stderr, Arguments delegate) {
        this.delegate = delegate;

        // Setup preliminary logging support before --logging, --debug, and other params are parsed.
        // This allows things like ArgumentReceivers to log when deprecated arguments are used.
        this.smithyHandler = new CliLogHandler(new BasicFormatter(), colors, stderr);
        rootLogger.addHandler(smithyHandler);
        restoreFunctions.add(() -> rootLogger.removeHandler(smithyHandler));

        // Remove existing console handlers.
        for (Handler h : rootLogger.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                rootLogger.removeHandler(h);
                restoreFunctions.add(() -> rootLogger.addHandler(h));
            }
        }
    }

    @Override
    public void addReceiver(ArgumentReceiver receiver) {
        delegate.addReceiver(receiver);
    }

    @Override
    public boolean hasReceiver(Class<? extends ArgumentReceiver> receiverClass) {
        return delegate.hasReceiver(receiverClass);
    }

    @Override
    public <T extends ArgumentReceiver> T getReceiver(Class<T> type) {
        return delegate.getReceiver(type);
    }

    @Override
    public Iterable<ArgumentReceiver> getReceivers() {
        return delegate.getReceivers();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public String peek() {
        return delegate.peek();
    }

    @Override
    public String shift() {
        return delegate.shift();
    }

    @Override
    public String shiftFor(String parameter) {
        return delegate.shiftFor(parameter);
    }

    @Override
    public List<String> getPositional() {
        List<String> result = delegate.getPositional();

        if (!configuredLogging) {
            configuredLogging = true;
            configureLogging(delegate.getReceiver(StandardOptions.class));
        }

        return result;
    }

    void restoreLogging() {
        for (Runnable runnable : restoreFunctions) {
            runnable.run();
        }
        restoreFunctions.clear();
    }

    private void configureLogging(StandardOptions options) {
        Level level = options.logging();

        // Be sure to restore the rootLevel logging when done with the CLI.
        Level previousRootLevel = rootLogger.getLevel();
        restoreFunctions.add(() -> rootLogger.setLevel(previousRootLevel));
        rootLogger.setLevel(level);

        // Set the log level on each handler attached to the root handler.
        for (Handler h : rootLogger.getHandlers()) {
            if (h.getLevel() != level) {
                // Change the log level if needed.
                Level currentLevel = h.getLevel();
                restoreFunctions.add(() -> h.setLevel(currentLevel));
                h.setLevel(level);
            }
        }

        if (level == Level.OFF) {
            // If logging was disabled with --logging OFF, then remove the smithy logger.
            rootLogger.removeHandler(smithyHandler);
        } else if (options.debug()) {
            smithyHandler.setFormatter(new DebugFormatter());
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
        private final ColorFormatter colors;
        private final CliPrinter printer;

        CliLogHandler(Formatter formatter, ColorFormatter colors, CliPrinter printer) {
            this.colors = colors;
            this.printer = printer;
            setFormatter(formatter);
        }

        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                String formatted = getFormatter().format(record);
                if (record.getLevel().equals(Level.SEVERE)) {
                    colors.println(printer, formatted, ColorTheme.ERROR);
                } else if (record.getLevel().equals(Level.WARNING)) {
                    colors.println(printer, formatted, ColorTheme.WARNING);
                } else {
                    printer.println(formatted);
                }
                // We want to see log messages right away, so flush the printer.
                printer.flush();
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }
}

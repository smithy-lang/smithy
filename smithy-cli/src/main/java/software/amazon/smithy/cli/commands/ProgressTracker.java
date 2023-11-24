/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli.commands;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.smithy.cli.Command;

final class ProgressTracker implements AutoCloseable {
    private static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(1, runnable -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setDaemon(true);
        return thread;
    });
    private static final long INTERVAL_MILLIS = 400L;
    private final ScheduledFuture<?> task;
    private final ProgressStyle style;
    private final Command.Env env;
    private final AtomicInteger tracker;
    private final boolean quiet;

    ProgressTracker(Command.Env env, ProgressStyle style, boolean quiet) {
        this(env, style, quiet, new AtomicInteger());
    }

    ProgressTracker(Command.Env env, ProgressStyle style, boolean quiet, AtomicInteger tracker) {
        this.env = env;
        this.style = style;
        this.quiet = quiet;
        this.tracker = tracker;

        // Do not print a progress bar if the quiet setting is enabled
        if (quiet) {
            task = null;
        } else {
            task = EXECUTOR.scheduleAtFixedRate(this::write, 0, INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void close() {
        if (!quiet) {
            task.cancel(false);
            try {
                EXECUTOR.schedule(this::executeClose, 0, TimeUnit.NANOSECONDS).get();
            } catch (ExecutionException | InterruptedException e) { /* ignored */ }
        }
    }

    private void write() {
        style.updateAction(env, tracker);
        // Flush so the output is written immediately
        env.stdout().flush();
    }

    private void executeClose() {
        style.closeAction(env);
        // Flush so the output is written immediately
        env.stdout().flush();
    }
}

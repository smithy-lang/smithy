/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ProgressTracker implements AutoCloseable {
    private static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(1, runnable -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setDaemon(true);
        return thread;
    });
    private static final long INTERVAL_MILLIS = 400L;
    private final ScheduledFuture<?> task;
    private final ProgressStyle style;
    private final Command.Env env;

    public ProgressTracker(Command.Env env, ProgressStyle style) {
        this.env = env;
        this.style = style;
        task = EXECUTOR.scheduleAtFixedRate(this::write, 0, INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        task.cancel(false);
        try {
            EXECUTOR.schedule(this::executeClose, 0, TimeUnit.NANOSECONDS).get();
        } catch (ExecutionException | InterruptedException e) { /* ignored */ }
    }

    private void write() {
        style.updateAction(env);
        // Flush so the output is written immediately
        env.stdout().flush();
    }

    private void executeClose() {
        style.closeAction(env);
        // Flush so the output is written immediately
        env.stdout().flush();
    }
}

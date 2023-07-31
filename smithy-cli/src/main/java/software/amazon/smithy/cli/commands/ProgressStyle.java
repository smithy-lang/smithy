/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli.commands;

import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.utils.StringUtils;


interface ProgressStyle {

    void updateAction(Command.Env env, AtomicInteger tracker);

    void closeAction(Command.Env env);

    static ColorBuffer getBuffer(Command.Env env) {
        return ColorBuffer.of(env.colors(), env.stdout());
    }

    static ProgressStyle dots(String progressMessage, String closeMessage) {
        return new ProgressStyle() {
            private static final String PROGRESS_CHAR = ".";
            private static final int TICKER_LENGTH = 3;
            private final long startTimeMillis = System.currentTimeMillis();

            @Override
            public void updateAction(Command.Env env, AtomicInteger tracker) {
                int tickCount = tracker.getAndIncrement();
                int tickNumber =  tickCount % (TICKER_LENGTH + 1);
                String loadStr = StringUtils.repeat(PROGRESS_CHAR, tickNumber)
                        + StringUtils.repeat(" ", TICKER_LENGTH - tickNumber);
                try (ColorBuffer buffer = getBuffer(env)) {
                    buffer.print("\r")
                            .print(progressMessage, ColorTheme.NOTE)
                            .print(loadStr, ColorTheme.NOTE);
                }
            }

            @Override
            public void closeAction(Command.Env env) {
                try (ColorBuffer buffer = getBuffer(env)) {
                    buffer.print("\r")
                            .print(closeMessage, ColorTheme.SUCCESS)
                            .print(" [", ColorTheme.MUTED)
                            .print((System.currentTimeMillis() - startTimeMillis) / 1000.0 + "s",
                                    ColorTheme.NOTE)
                            .print("]", ColorTheme.MUTED)
                            .println();
                }
            }
        };
    }
}

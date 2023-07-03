/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli;

import software.amazon.smithy.utils.StringUtils;


public interface ProgressStyle {

    void updateAction(Command.Env env);

    void closeAction(Command.Env env);

    static ColorBuffer getBuffer(Command.Env env) {
        return ColorBuffer.of(env.colors(), env.stdout());
    }

    static ProgressStyle dots(String progressMessage, String closeMessage) {
        return new ProgressStyle() {
            private static final String PROGRESS_CHAR = ".";
            private static final int TICKER_LENGTH = 3;

            private int totalTicks = 0;
            private final long startTimeMillis = System.currentTimeMillis();

            @Override
            public void updateAction(Command.Env env) {
                int tickNumber = totalTicks % (TICKER_LENGTH + 1);
                String loadStr = StringUtils.repeat(PROGRESS_CHAR, tickNumber)
                        + StringUtils.repeat(" ", TICKER_LENGTH - tickNumber);
                try (ColorBuffer buffer = getBuffer(env)) {
                    buffer.print("\r")
                            .print(progressMessage, ColorTheme.NOTE)
                            .print(loadStr, ColorTheme.NOTE);
                }
                totalTicks += 1;
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

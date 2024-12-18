/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

/**
 * Standardizes on colors across commands.
 */
public final class ColorTheme {

    public static final Style EM_UNDERLINE = Style.of(Style.BRIGHT_WHITE, Style.UNDERLINE);
    public static final Style DEPRECATED = Style.of(Style.BG_YELLOW, Style.BLACK);

    public static final Style MUTED = Style.of(Style.BRIGHT_BLACK);
    public static final Style EVENT_SHAPE_ID = Style.of(Style.BRIGHT_MAGENTA);
    public static final Style LITERAL = Style.of(Style.CYAN);

    public static final Style ERROR_TITLE = Style.of(Style.BG_RED, Style.BLACK);
    public static final Style ERROR = Style.of(Style.RED);

    public static final Style DANGER_TITLE = Style.of(Style.BG_MAGENTA, Style.BLACK);
    public static final Style DANGER = Style.of(Style.MAGENTA);

    public static final Style WARNING_TITLE = Style.of(Style.BG_YELLOW, Style.BLACK);
    public static final Style WARNING = Style.of(Style.YELLOW);

    public static final Style NOTE_TITLE = Style.of(Style.BG_CYAN, Style.BLACK);
    public static final Style NOTE = Style.of(Style.CYAN);

    public static final Style SUPPRESSED_TITLE = Style.of(Style.BG_GREEN, Style.BLACK);
    public static final Style SUPPRESSED = Style.of(Style.GREEN);

    public static final Style SUCCESS = Style.of(Style.GREEN);

    public static final Style DIFF_TITLE = Style.of(Style.BG_BRIGHT_BLACK, Style.WHITE);
    public static final Style DIFF_EVENT_TITLE = Style.of(Style.BG_BRIGHT_BLUE, Style.BLACK);

    public static final Style HINT_TITLE = Style.of(Style.BRIGHT_GREEN);

    public static final Style TEMPLATE_TITLE = Style.of(Style.BOLD, Style.BRIGHT_WHITE);
    public static final Style TEMPLATE_LIST_BORDER = Style.of(Style.BRIGHT_WHITE);

    private ColorTheme() {}
}

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

/**
 * Parameters used to change the ANSI public style of text.
 */
public enum Style {
    BOLD(1),
    FAINT(2),
    ITALIC(3),
    UNDERLINE(4),

    BLACK(30),
    RED(31),
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    WHITE(37),

    BRIGHT_BLACK(90),
    BRIGHT_RED(91),
    BRIGHT_GREEN(92),
    BRIGHT_YELLOW(93),
    BRIGHT_BLUE(94),
    BRIGHT_MAGENTA(95),
    BRIGHT_CYAN(96),
    BRIGHT_WHITE(97),

    BG_BLACK(40),
    BG_RED(41),
    BG_GREEN(42),
    BG_YELLOW(43),
    BG_BLUE(44),
    BG_MAGENTA(45),
    BG_CYAN(46),
    BG_WHITE(47),

    BG_BRIGHT_BLACK(100),
    BG_BRIGHT_RED(101),
    BG_BRIGHT_GREEN(102),
    BG_BRIGHT_YELLOW(103),
    BG_BRIGHT_BLUE(104),
    BG_BRIGHT_MAGENTA(105),
    BG_BRIGHT_CYAN(106),
    BG_BRIGHT_WHITE(107);

    private final String code;

    Style(int code) {
        this.code = String.valueOf(code);
    }

    @Override
    public String toString() {
        return code;
    }
}

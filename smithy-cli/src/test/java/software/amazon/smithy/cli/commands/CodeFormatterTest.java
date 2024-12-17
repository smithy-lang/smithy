/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.AnsiColorFormatter;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.model.loader.sourcecontext.SourceContextLoader;

public class CodeFormatterTest {

    private final String ls = System.lineSeparator();
    private final String ls2 = ls + ls;

    @Test
    public void outputsSequentialLinesWithNoCursor() {
        StringBuilder builder = new StringBuilder();
        ColorFormatter colors = AnsiColorFormatter.NO_COLOR;
        CodeFormatter formatter = new CodeFormatter(ColorBuffer.of(colors, builder), 80);
        List<SourceContextLoader.Line> lines = Arrays.asList(
                new SourceContextLoader.Line(2, "A"),
                new SourceContextLoader.Line(3, "B"),
                new SourceContextLoader.Line(4, "C"),
                new SourceContextLoader.Line(5, "D"));

        formatter.writeCode(0, 0, lines);

        assertThat(builder.toString(),
                equalTo("2| A" + ls
                        + "3| B" + ls
                        + "4| C" + ls
                        + "5| D" + ls2));
    }

    @Test
    public void outputsSequentialLinesWithCursor() {
        StringBuilder builder = new StringBuilder();
        ColorFormatter colors = AnsiColorFormatter.NO_COLOR;
        CodeFormatter formatter = new CodeFormatter(ColorBuffer.of(colors, builder), 80);
        List<SourceContextLoader.Line> lines = Arrays.asList(
                new SourceContextLoader.Line(2, "Aa"),
                new SourceContextLoader.Line(3, "Bb"),
                new SourceContextLoader.Line(4, "Cc"),
                new SourceContextLoader.Line(5, "Dd"));

        formatter.writeCode(3, 2, lines);

        assertThat(builder.toString(),
                equalTo("2| Aa" + ls
                        + "3| Bb" + ls
                        + " |  ^" + ls
                        + "4| Cc" + ls
                        + "5| Dd" + ls2));
    }

    @Test
    public void detectsLineSkips() {
        StringBuilder builder = new StringBuilder();
        ColorFormatter colors = AnsiColorFormatter.NO_COLOR;
        CodeFormatter formatter = new CodeFormatter(ColorBuffer.of(colors, builder), 80);
        List<SourceContextLoader.Line> lines = Arrays.asList(
                new SourceContextLoader.Line(2, "Aa"),
                new SourceContextLoader.Line(8, "Bb"),
                new SourceContextLoader.Line(9, "Cc"),
                new SourceContextLoader.Line(12, "Dd"));

        formatter.writeCode(8, 2, lines);

        assertThat(builder.toString(),
                equalTo("2 | Aa" + ls
                        + "··|" + ls
                        + "8 | Bb" + ls
                        + "  |  ^" + ls
                        + "9 | Cc" + ls
                        + "··|" + ls
                        + "12| Dd" + ls2));
    }

    @Test
    public void truncatesLongLines() {
        StringBuilder builder = new StringBuilder();
        ColorFormatter colors = AnsiColorFormatter.NO_COLOR;
        CodeFormatter formatter = new CodeFormatter(ColorBuffer.of(colors, builder), 10);
        List<SourceContextLoader.Line> lines = Collections.singletonList(
                new SourceContextLoader.Line(1, "abcdefghijklmnopqrstuvwxyz"));

        formatter.writeCode(0, 0, lines);

        assertThat(builder.toString(), equalTo("1| abcdefghi…" + ls2));
    }

    @Test
    public void ignoresEmptyLines() {
        StringBuilder builder = new StringBuilder();
        ColorFormatter colors = AnsiColorFormatter.NO_COLOR;
        CodeFormatter formatter = new CodeFormatter(ColorBuffer.of(colors, builder), 80);
        List<SourceContextLoader.Line> lines = Collections.emptyList();

        formatter.writeCode(0, 0, lines);

        assertThat(builder.toString(), equalTo(""));
    }
}

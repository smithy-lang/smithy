/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.AnsiColorFormatter;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.sourcecontext.SourceContextLoader;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class PrettyAnsiValidationFormatterTest {
    @Test
    public void formatsEventsWithNoColors() {
        PrettyAnsiValidationFormatter pretty = createFormatter(AnsiColorFormatter.NO_COLOR);
        String formatted = formatTestEventWithSeverity(pretty, Severity.ERROR);

        assertThat(formatted, equalTo(
                "\n"
                + "──  ERROR  ───────────────────────────────────────────────────────────────── Foo\n"
                + "Shape: smithy.example#Foo\n"
                + "File:  build/resources/test/software/amazon/smithy/cli/commands/valid-model.smithy:5:1\n"
                + "\n"
                + "4| \n"
                + "5| resource Foo {\n"
                + " | ^\n"
                + "\n"
                + "Hello, `there`\n")); // keeps ticks because formatting is disabled.
    }

    @Test
    public void formatsEventsWithColors() {
        PrettyAnsiValidationFormatter pretty = createFormatter(AnsiColorFormatter.FORCE_COLOR);
        String formatted = formatTestEventWithSeverity(pretty, Severity.ERROR);

        assertThat(formatted, equalTo(
                "\n"
                + "\u001B[31m── \u001B[0m\u001B[41;30m ERROR \u001B[0m\u001B[31m ───────────────────────────────────────────────────────────────── \u001B[0mFoo\n"
                + "\u001B[90mShape: \u001B[0m\u001B[34msmithy.example#Foo\u001B[0m\n"
                + "\u001B[90mFile:  \u001B[0m\u001B[34mbuild/resources/test/software/amazon/smithy/cli/commands/valid-model.smithy:5:1\u001B[0m\n"
                + "\n"
                + "\u001B[90m4\u001B[90m| \u001B[0m\u001B[0m\n"
                + "\u001B[90m5\u001B[90m| \u001B[0m\u001B[0mresource Foo {\n"
                + "\u001B[90m |\u001B[0m \u001B[31m^\u001B[0m\n"
                + "\n"
                + "Hello, \u001B[36mthere\u001B[0m\n"));
    }

    @Test
    public void doesNotIncludeSourceLocationNoneInOutput() {
        PrettyAnsiValidationFormatter pretty = createFormatter(AnsiColorFormatter.NO_COLOR);
        ValidationEvent event = ValidationEvent.builder()
                .id("Hello")
                .severity(Severity.WARNING)
                .shapeId(ShapeId.from("smithy.example#Foo"))
                .message("Hi")
                .build();

        String formatted =  normalizeLinesAndFiles(pretty.format(event));

        assertThat(formatted, equalTo(
                "\n"
                + "──  WARNING  ───────────────────────────────────────────────────────────── Hello\n"
                + "Shape: smithy.example#Foo\n"
                + "\n"
                + "Hi\n"));
    }

    @Test
    public void wrapsLongLines() {
        PrettyAnsiValidationFormatter pretty = createFormatter(AnsiColorFormatter.NO_COLOR);
        ValidationEvent event = ValidationEvent.builder()
                .id("Hello")
                .severity(Severity.WARNING)
                .shapeId(ShapeId.from("smithy.example#Foo"))
                .message("abcdefghijklmnopqrstuvwxyz 1234567890 abcdefghijklmnopqrstuvwxyz 1234567890 "
                         + "abcdefghijklmnopqrstuvwxyz 1234567890 abcdefghijklmnopqrstuvwxyz 1234567890")
                .build();

        String formatted =  normalizeLinesAndFiles(pretty.format(event));

        assertThat(formatted, equalTo(
                "\n"
                + "──  WARNING  ───────────────────────────────────────────────────────────── Hello\n"
                + "Shape: smithy.example#Foo\n"
                + "\n"
                + "abcdefghijklmnopqrstuvwxyz 1234567890 abcdefghijklmnopqrstuvwxyz 1234567890\n"
                + "abcdefghijklmnopqrstuvwxyz 1234567890 abcdefghijklmnopqrstuvwxyz 1234567890\n"));
    }

    private PrettyAnsiValidationFormatter createFormatter(ColorFormatter colors) {
        SourceContextLoader loader = SourceContextLoader.createLineBasedLoader(2);
        return new PrettyAnsiValidationFormatter(loader, colors);
    }

    private String formatTestEventWithSeverity(PrettyAnsiValidationFormatter pretty, Severity severity) {
        Model model = Model.assembler().addImport(getClass().getResource("valid-model.smithy")).assemble().unwrap();
        ValidationEvent event = ValidationEvent.builder()
                .id("Foo")
                .severity(severity)
                .shape(model.expectShape(ShapeId.from("smithy.example#Foo")))
                .message("Hello, `there`")
                .build();
        return normalizeLinesAndFiles(pretty.format(event));
    }

    private String normalizeLinesAndFiles(String output) {
        return output.replace("\r\n", "\n").replace("\r", "\n").replace("\\", "/");
    }

    @Test
    public void toleratesInvalidSourceFiles() {
        ColorFormatter colors = AnsiColorFormatter.NO_COLOR;
        SourceContextLoader loader = s -> {
            throw new UncheckedIOException(new IOException("Error!!!"));
        };
        PrettyAnsiValidationFormatter pretty = new PrettyAnsiValidationFormatter(loader, colors);
        ValidationEvent event = ValidationEvent.builder()
                .id("Foo")
                .severity(Severity.NOTE)
                .sourceLocation(new SourceLocation("/foo", 1, 1))
                .message("Hello")
                .build();

        String result = normalizeLinesAndFiles(pretty.format(event));

        assertThat(result, equalTo(
                "\n"
                + "──  NOTE  ────────────────────────────────────────────────────────────────── Foo\n"
                + "File:  /foo:1:1\n"
                + "\n"
                + "Invalid source file: Error!!!\n"
                + "\n"
                + "Hello\n"));
    }
}

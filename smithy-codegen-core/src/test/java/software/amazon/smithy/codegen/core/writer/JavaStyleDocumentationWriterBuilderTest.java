/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.writer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Locale;
import org.junit.jupiter.api.Test;

public class JavaStyleDocumentationWriterBuilderTest {
    @Test
    public void writesAndEscapesBlockQuotes() {
        MyWriter writer = new MyWriter("foo");
        DocumentationWriter<MyWriter> docWriter = new JavaStyleDocumentationWriterBuilder().build();
        docWriter.writeDocs(writer, () -> {
            writer.write("Hello.");
            writer.write("*/");
            writer.write("Goodbye.");
        });

        assertThat(writer.toString(),
                equalTo("/**\n * Hello.\n * *\\/\n * Goodbye.\n */\n"));
    }

    @Test
    public void canSetCustomSectionName() {
        MyWriter writer = new MyWriter("foo");
        DocumentationWriter<MyWriter> docWriter = new JavaStyleDocumentationWriterBuilder()
                .namedDocumentationSection("docs")
                .build();
        writer.onSection("docs", contents -> {
            writer.writeInlineWithNoFormatting(contents.toString().toUpperCase(Locale.ENGLISH));
        });
        docWriter.writeDocs(writer, () -> {
            writer.write("Hello");
        });

        assertThat(writer.toString(),
                equalTo("/**\n * HELLO\n */\n"));
    }

    @Test
    public void ensuresNewlineIsAddedBeforeClosing() {
        MyWriter writer = new MyWriter("foo");
        DocumentationWriter<MyWriter> docWriter = new JavaStyleDocumentationWriterBuilder()
                .namedDocumentationSection("docs")
                .build();
        writer.onSection("docs", contents -> {
            writer.writeInlineWithNoFormatting(contents.toString().toUpperCase(Locale.ENGLISH));
        });
        docWriter.writeDocs(writer, () -> {
            writer.writeInline("Hello");
        });

        assertThat(writer.toString(),
                equalTo("/**\n * HELLO\n */\n"));
    }

    @Test
    public void canSetCustomMappingFunction() {
        MyWriter writer = new MyWriter("foo");
        DocumentationWriter<MyWriter> docWriter = new JavaStyleDocumentationWriterBuilder()
                .mappingFunction(s -> s.toUpperCase(Locale.ENGLISH))
                .build();
        docWriter.writeDocs(writer, () -> {
            writer.write("Hello");
        });

        assertThat(writer.toString(),
                equalTo("/**\n * HELLO\n */\n"));
    }

    @Test
    public void canEscapeAt() {
        MyWriter writer = new MyWriter("foo");
        DocumentationWriter<MyWriter> docWriter = new JavaStyleDocumentationWriterBuilder()
                .escapeAtSignWithEntity(true)
                .build();
        docWriter.writeDocs(writer, () -> {
            writer.write("Hello @foo");
        });

        assertThat(writer.toString(),
                equalTo("/**\n * Hello &#064;foo\n */\n"));
    }

    @Test
    public void canEscapeAtWithComposedCustomEscaper() {
        MyWriter writer = new MyWriter("foo");
        DocumentationWriter<MyWriter> docWriter = new JavaStyleDocumentationWriterBuilder()
                .mappingFunction(s -> s.toUpperCase(Locale.ENGLISH))
                .escapeAtSignWithEntity(true)
                .build();
        docWriter.writeDocs(writer, () -> {
            writer.write("Hello @foo");
        });

        assertThat(writer.toString(),
                equalTo("/**\n * HELLO &#064;FOO\n */\n"));
    }
}

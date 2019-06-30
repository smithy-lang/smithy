/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CodeWriterTest {
    @Test
    public void limitsBlankLines() {
        CodeWriter writer = new CodeWriter().trimBlankLines().trimTrailingSpaces();
        writer.write("if (%s == \"foo\") {\n\n\n\n", "BAZ")
                .indent()
                .write("print(%s)", "BAZ")
                .dedent()
                .write("}");

        assertThat(writer.toString(), equalTo("if (BAZ == \"foo\") {\n\n    print(BAZ)\n}\n"));
    }

    @Test
    public void doesNotLimitBlankLines() {
        CodeWriter writer = new CodeWriter().trimTrailingSpaces();
        writer.write("if (%s == \"foo\") {\n\n\n\n", "BAZ")
                .indent()
                .write("print(%s)", "BAZ")
                .dedent()
                .write("}");

        assertThat(writer.toString(), equalTo("if (BAZ == \"foo\") {\n\n\n\n\n    print(BAZ)\n}\n"));
    }

    @Test
    public void resetsBlankLineCounterWhenContentAppears() {
        CodeWriter writer = new CodeWriter().trimBlankLines().trimTrailingSpaces();
        writer.write(".\n.\n.\n\n.\n\n\n.");

        assertThat(writer.toString(), equalTo(".\n.\n.\n\n.\n\n.\n"));
    }

    @Test
    public void trimsTrailingSpaces() {
        CodeWriter writer = new CodeWriter().trimBlankLines().trimTrailingSpaces();
        writer.write("hello ").writeInline("there  ");

        assertThat(writer.toString(), equalTo("hello\nthere"));
    }

    @Test
    public void trimsSpacesAndBlankLines() {
        CodeWriter writer = new CodeWriter().trimTrailingSpaces().trimBlankLines();
        writer.write("hello\n\n\nthere, ").writeInline("bud");

        assertThat(writer.toString(), equalTo("hello\n\nthere,\nbud"));
    }

    @Test
    public void canInsertTrailingNewlines() {
        CodeWriter writer = new CodeWriter().trimTrailingSpaces().trimBlankLines();
        writer.write("hello\n\n\nthere, ").writeInline("bud");

        assertThat(writer.toString(), equalTo("hello\n\nthere,\nbud"));
    }

    @Test
    public void canWriteTextWithNewlinePrefixAndBlankLineTrimming() {
        CodeWriter writer = CodeWriter.createDefault();
        writer
                .write("/**")
                .setNewlinePrefix(" * ")
                .write("This is some docs.")
                .write("And more docs.\n\n\n")
                .write("Foo.")
                .setNewlinePrefix("")
                .write(" */");

        /* Becomes:
         *
         *
         * /**
         *   * This is some docs.
         *   * And more docs.
         *   *
         *   * Foo.
         *   *\/
         *
         *    ^ Minus this character.
         */
        assertThat(
                writer.toString(),
                equalTo("/**\n * This is some docs.\n * And more docs.\n * \n * Foo.\n */\n"));
    }

    @Test
    public void handlesIndentation() {
        CodeWriter writer = CodeWriter.createDefault();
        writer
                .write("Hi")
                    .indent()
                    .write("A")
                    .indent()
                        .write("B")
                        .dedent()
                    .write("C")
                    .dedent()
                .write("Fin.");

        assertThat(
                writer.toString(),
                equalTo("Hi\n    A\n        B\n    C\nFin.\n"));
    }

    @Test
    public void cannotDedentPastRoot() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CodeWriter writer = CodeWriter.createDefault();
            writer.dedent(10);
        });
    }

    @Test
    public void canDedentToRoot() {
        CodeWriter writer = CodeWriter.createDefault().indent(10).dedent(-1).write("Hi");

        assertThat(writer.toString(), equalTo("Hi\n"));
    }

    @Test
    public void canIndentDocBlocks() {
        CodeWriter writer = CodeWriter.createDefault();
        writer.indent()
                .write("/**")
                .setNewlinePrefix(" * ")
                .write("This is some docs.")
                .write("And more docs.\n\n\n")
                .write("Foo.")
                .setNewlinePrefix("")
                .write(" */")
                .dedent();

        /* Becomes:
         *
         *
         *     /**
         *       * This is some docs.
         *       * And more docs.
         *       *
         *       * Foo.
         *       *\/
         *
         *        ^ Minus this character.
         */
        assertThat(
                writer.toString(),
                equalTo("    /**\n     * This is some docs.\n     * And more docs.\n     * \n     * Foo.\n     */\n"));
    }

    @Test
    public void injectsNewlineWhenNeeded() {
        CodeWriter writer = CodeWriter.createDefault();
        writer.writeInline("foo");

        assertThat(writer.toString(), equalTo("foo\n"));
    }

    @Test
    public void doesNotInjectNewlineWhenNotNeeded() {
        CodeWriter writer = CodeWriter.createDefault();
        writer.write("foo");

        assertThat(writer.toString(), equalTo("foo\n"));
    }

    @Test
    public void doesNotInjectNewlineWhenNotNeededThroughInline() {
        CodeWriter writer = CodeWriter.createDefault();
        writer.writeInline("foo\n");

        assertThat(writer.toString(), equalTo("foo\n"));
    }

    @Test
    public void cannotPopMoreStatesThanExist() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CodeWriter.createDefault()
                    .pushState()
                    .popState()
                    .popState();
        });
    }

    @Test
    public void canPushAndPopState() {
        CodeWriter writer = CodeWriter.createDefault();
        writer
                .setNewlinePrefix("0: ")
                .write("Hi")
                .pushState()
                .indent(2)
                    .setNewlinePrefix("2: ")
                    .write("there,")
                    .pushState()
                    .indent(2)
                        .setNewlinePrefix("4: ")
                        .write("guy")
                        .popState()
                    .write("Foo")
                    .popState()
                .write("baz");

        assertThat(
                writer.toString(),
                equalTo("0: Hi\n        2: there,\n                4: guy\n        2: Foo\n0: baz\n"));
    }

    @Test
    public void canChangeFormatter() {
        String result = CodeWriter.createDefault()
                .setFormatter((text, args) -> text.replace("{}", "hi"))
                .write("lorem {} dolor {}", "ipsum", "qux")
                .toString();

        assertThat(result, equalTo("lorem hi dolor hi\n"));
    }

    @Test
    public void writesBlocks() {
        String result = CodeWriter.createDefault()
                .openBlock("public final class %s {", "Foo")
                    .openBlock("public void main(String[] args) {")
                        .write("System.out.println(args[0]);")
                    .closeBlock("}")
                .closeBlock("}")
                .toString();

        assertThat(result, equalTo("public final class Foo {\n    public void main(String[] args) {\n        System.out.println(args[0]);\n    }\n}\n"));
    }
}

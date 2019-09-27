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

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CodeWriterTest {
    @Test
    public void limitsBlankLines() {
        CodeWriter writer = new CodeWriter().trimBlankLines().trimTrailingSpaces();
        writer.write("if ($L == \"foo\") {\n\n\n\n", "BAZ")
                .indent()
                .write("print($L)", "BAZ")
                .dedent()
                .write("}");

        assertThat(writer.toString(), equalTo("if (BAZ == \"foo\") {\n\n    print(BAZ)\n}\n"));
    }

    @Test
    public void doesNotLimitBlankLines() {
        CodeWriter writer = new CodeWriter().trimTrailingSpaces();
        writer.write("if ($L == \"foo\") {\n\n\n\n", "BAZ")
                .indent()
                .write("print($L)", "BAZ")
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
        writer.write("hello there  ");

        assertThat(writer.toString(), equalTo("hello there\n"));
    }

    @Test
    public void trimsSpacesAndBlankLines() {
        CodeWriter writer = new CodeWriter().trimTrailingSpaces().trimBlankLines();
        writer.write("hello\n\n\nthere, bud");

        assertThat(writer.toString(), equalTo("hello\n\nthere, bud\n"));
    }

    @Test
    public void insertsTrailingNewlines() {
        CodeWriter writer = new CodeWriter().trimTrailingSpaces().trimBlankLines();
        writer.write("hello there, bud");

        assertThat(writer.toString(), equalTo("hello there, bud\n"));
    }

    @Test
    public void canWriteTextWithNewlinePrefixAndBlankLineTrimming() {
        CodeWriter writer = CodeWriter.createDefault();
        writer
                .write("/**")
                .setNewlinePrefix(" * ")
                .write("This is some docs.")
                .write("And more docs.\n")
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
                equalTo("/**\n * This is some docs.\n * And more docs.\n *\n * Foo.\n */\n"));
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
                .write("And more docs.\n")
                .write("Foo.")
                .setNewlinePrefix("")
                .write(" */")
                .dedent();

        /* Becomes:
         *
         *
         *      /**
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
                equalTo("    /**\n     * This is some docs.\n     * And more docs.\n     *\n     * Foo.\n     */\n"));
    }

    @Test
    public void injectsNewlineWhenNeeded() {
        CodeWriter writer = CodeWriter.createDefault();
        writer.write("foo");

        assertThat(writer.toString(), equalTo("foo\n"));
    }

    @Test
    public void doesNotInjectNewlineWhenNotNeeded() {
        CodeWriter writer = CodeWriter.createDefault();
        writer.write("foo");

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
    public void writesBlocks() {
        String result = CodeWriter.createDefault()
                .openBlock("public final class $L {", "Foo")
                    .openBlock("public void main(String[] args) {")
                        .write("System.out.println(args[0]);")
                    .closeBlock("}")
                .closeBlock("}")
                .toString();

        assertThat(result, equalTo("public final class Foo {\n    public void main(String[] args) {\n        System.out.println(args[0]);\n    }\n}\n"));
    }

    @Test
    public void supportsCall() {
        CodeWriter w = CodeWriter.createDefault();
        w.call(() -> w.write("Hello!"));

        assertThat(w.toString(), equalTo("Hello!\n"));
    }

    @Test
    public void doesNotWriteNullOptionally() {
        CodeWriter w = CodeWriter.createDefault().insertTrailingNewline(false);
        w.writeOptional(null);

        assertThat(w.toString(), equalTo(""));
    }

    @Test
    public void doesNotWriteEmptyOptionals() {
        CodeWriter w = CodeWriter.createDefault().insertTrailingNewline(false);
        w.writeOptional(Optional.empty());

        assertThat(w.toString(), equalTo(""));
    }

    @Test
    public void doesNotWriteEmptyStrings() {
        CodeWriter w = CodeWriter.createDefault().insertTrailingNewline(false);
        w.writeOptional("");

        assertThat(w.toString(), equalTo(""));
    }

    @Test
    public void doesNotWriteOptionalsThatContainEmptyStrings() {
        CodeWriter w = CodeWriter.createDefault().insertTrailingNewline(false);
        w.writeOptional(Optional.of(""));

        assertThat(w.toString(), equalTo(""));
    }

    @Test
    public void writesOptionalsWithNonEmptyStringValues() {
        CodeWriter w = CodeWriter.createDefault().insertTrailingNewline(false);
        w.writeOptional(Optional.of("hi!"));

        assertThat(w.toString(), equalTo("hi!"));
    }

    @Test
    public void writesLiteralOptionalByUnwrappingIt() {
        CodeWriter w = CodeWriter.createDefault();
        w.write("$L", Optional.of("hi!"));
        w.write("$L", Optional.empty());
        w.write("$S", Optional.of("hi!"));
        w.write("$S", Optional.empty());

        assertThat(w.toString(), equalTo("hi!\n\n\"hi!\"\n\"\"\n"));
    }

    @Test
    public void formatsNullAsEmptyString() {
        CodeWriter w = CodeWriter.createDefault();
        w.write("$L", (String) null);
        w.write("$S", (String) null);

        assertThat(w.toString(), equalTo("\n\"\"\n"));
    }

    @Test
    public void writesWithNamedContext() {
        CodeWriter w = CodeWriter.createDefault()
                .putContext("foo", "Hello!")
                .pushState()
                .putContext("foo", "Hola!")
                .write("Hi. $foo:L")
                .popState()
                .write("Hi. $foo:L");

        assertThat(w.toString(), equalTo("Hi. Hola!\nHi. Hello!\n"));
    }

    @Test
    public void hasSections() {
        // Setup the code writer and section interceptors.
        CodeWriter w = CodeWriter.createDefault().putContext("testing", "123");

        w.onSection("foo", text -> w.write("Yes: " + text));
        w.onSection("foo", text -> w.write("Si: " + text));
        w.onSection("placeholder", text -> w.write("$testing:L"));

        // Emit sections with their original values.
        w.pushState("foo").write("Original!").popState();
        w.pushState("placeholder").popState();
        w.pushState("empty-placeholder").popState();

        assertThat(w.toString(), equalTo("Si: Yes: Original!\n123\n"));
    }

    @Test
    public void allowsForCustomFormatters() {
        // Setup the code writer and section interceptors.
        CodeWriter w = CodeWriter.createDefault();
        w.putFormatter('X', (text, indent) -> text.toString().replace("\n", "\n" + indent + indent));
        w.setIndentText(" ");
        w.indent(2);
        w.write("Hi.$X.Newline?", "^\n$");

        assertThat(w.toString(), equalTo("  Hi.^\n    $.Newline?\n"));
    }

    @Test
    public void canIntegrateSectionsWithComplexStates() {
        CodeWriter writer = CodeWriter.createDefault();

        writer.onSection("foo", text -> writer.write("Intercepted: " + text + "!\nYap!"));

        writer.onSection("indented", text -> {
            writer.indent();
            writer.write("This is indented, in flow of parent.\nYap!");
            writer.write("Yap?");
            writer.setNewlinePrefix("# ");
            writer.write("Has a prefix\nYes");
        });

        writer.pushState();
        writer.write("/**");
        writer.setNewlinePrefix(" * ");

        writer.pushState("no_interceptors");
        writer.write("Hi!");
        writer.popState();

        writer.write("");

        writer.pushState("foo");
        writer.write("baz");
        writer.popState();

        writer.pushState("indented").popState();

        writer.popState();
        writer.write(" */");

        assertThat(writer.toString(), equalTo(
                "/**\n"
                + " * Hi!\n"
                + " *\n"
                + " * Intercepted: baz!\n"
                + " * Yap!\n"
                + " *     This is indented, in flow of parent.\n"
                + " *     Yap!\n"
                + " *     Yap?\n"
                + " *     # Has a prefix\n"
                + " *     # Yes\n"
                + " */\n"));
    }

    @Test
    public void canIntegrateInlineSectionsWithComplexStates() {
        CodeWriter writer = CodeWriter.createDefault();

        writer.onSection("foo", text -> writer.write(text + "!\nYap!"));

        writer.onSection("indented", text -> {
            writer.indent();
            writer.write("This is indented, in flow of parent.\nYap!");
            writer.write("Yap?");
            writer.setNewlinePrefix("# ");
            writer.write("Has a prefix\nYes");
        });

        writer.pushState();
        writer.write("/**");
        writer.setNewlinePrefix(" * ");
        writer.write("Foo ${S@no_interceptors}", "foo!");
        writer.write("");
        writer.write("${S@foo}", "baz");
        writer.write("${L@indented}", "");
        writer.popState();
        writer.write(" */");

        assertThat(writer.toString(), equalTo(
                "/**\n"
                + " * Foo \"foo!\"\n"
                + " *\n"
                + " * \"baz\"!\n"
                + " * Yap!\n"
                + " *     This is indented, in flow of parent.\n"
                + " *     Yap!\n"
                + " *     Yap?\n"
                + " *     # Has a prefix\n"
                + " *     # Yes\n"
                + " */\n"));
    }

    @Test
    public void hasOpenBlockRunnable0() {
        CodeWriter writer = CodeWriter.createDefault();
        String result = writer.openBlock("public {", "}", () -> {
            writer.write("hi();");
        })
        .toString();

        assertThat(result, equalTo("public {\n    hi();\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable1() {
        CodeWriter writer = CodeWriter.createDefault();
        String result = writer.openBlock("public final class $L {", "}", "Foo", () -> {
            writer.openBlock("public void main(String[] args) {", "}", () -> {
                writer.write("System.out.println(args[0]);");
            });
        })
        .toString();

        assertThat(result, equalTo("public final class Foo {\n    public void main(String[] args) {\n        System.out.println(args[0]);\n    }\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable2() {
        CodeWriter writer = CodeWriter.createDefault();
        String result = writer.openBlock("public $L $L {", "}", "1", "2", () -> {
            writer.write("hi();");
        })
        .toString();

        assertThat(result, equalTo("public 1 2 {\n    hi();\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable3() {
        CodeWriter writer = CodeWriter.createDefault();
        String result = writer.openBlock("public $L $L $L {", "}", "1", "2", "3", () -> {
            writer.write("hi();");
        })
        .toString();

        assertThat(result, equalTo("public 1 2 3 {\n    hi();\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable4() {
        CodeWriter writer = CodeWriter.createDefault();
        String result = writer.openBlock("public $L $L $L $L {", "}", "1", "2", "3", "4", () -> {
            writer.write("hi();");
        })
        .toString();

        assertThat(result, equalTo("public 1 2 3 4 {\n    hi();\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable5() {
        CodeWriter writer = CodeWriter.createDefault();
        String result = writer.openBlock("public $L $L $L $L $L {", "}", "1", "2", "3", "4", "5", () -> {
            writer.write("hi();");
        })
        .toString();

        assertThat(result, equalTo("public 1 2 3 4 5 {\n    hi();\n}\n"));
    }
}

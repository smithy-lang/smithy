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

import java.util.Locale;
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
    public void toStringCanDisableTrimmingTrailingSpaces() {
        CodeWriter writer = new CodeWriter()
                .insertTrailingNewline(false)
                .trimTrailingSpaces(false)
                .writeInline("hi ");

        assertThat(writer.toString(), equalTo("hi "));
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
    public void trailingNewlineIsAddedToEmptyText() {
        CodeWriter writer = new CodeWriter().insertTrailingNewline();

        assertThat(writer.toString(), equalTo("\n"));
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
    public void canGetTypedContextValues() {
        CodeWriter w = new CodeWriter();
        w.putContext("foo", "hello");
        String value = w.getContext("foo", String.class);

        assertThat(value, equalTo("hello"));
    }

    @Test
    public void failsWhenTypedContextDoesNotMatch() {
        CodeWriter w = new CodeWriter();
        w.pushState("a");
        w.putContext("foo", "hello");
        w.write("Hello {");

        ClassCastException e = Assertions.assertThrows(ClassCastException.class, () -> {
            w.getContext("foo", Integer.class);
        });

        assertThat(e.getMessage(), equalTo("Expected CodeWriter context value 'foo' to be an instance of "
                                           + "java.lang.Integer, but found java.lang.String "
                                           + "(Debug Info {path=ROOT/a, near=Hello {\\n})"));
    }

    @Test
    public void getsDebugInfoWithNoLines() {
        CodeWriter w = new CodeWriter();

        assertThat(w.getDebugInfo().toString(), equalTo("(Debug Info {path=ROOT, near=})"));
    }

    @Test
    public void getsDebugInfoWithNoLinesAndContext() {
        CodeWriter w = new CodeWriter();
        w.pushState("a");
        w.pushState("b");

        assertThat(w.getDebugInfo().toString(), equalTo("(Debug Info {path=ROOT/a/b, near=})"));
    }

    @Test
    public void getsDebugInfoWithTwoLines() {
        CodeWriter w = new CodeWriter();
        w.write("Hello {");
        w.write("  hello");

        assertThat(w.getDebugInfo().toString(), equalTo("(Debug Info {path=ROOT, near=Hello {\\n  hello\\n})"));
    }

    @Test
    public void getsDebugInfoWithThreeLines() {
        CodeWriter w = new CodeWriter();
        w.write("Hello {");
        w.write("  hello1");
        w.write("  hello2");

        assertThat(w.getDebugInfo().toString(), equalTo("(Debug Info {path=ROOT, near=  hello1\\n  hello2\\n})"));
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
    public void canPrependAndAppendToSection() {
        CodeWriter w = CodeWriter.createDefault().putContext("testing", "123");
        w.onSectionPrepend("foo", () -> w.write("A"));
        w.onSection("foo", text -> {
            w.write(text);
            w.write("C");
        });
        w.onSectionAppend("foo", () -> w.write("D"));
        w.pushState("foo").write("B").popState();

        assertThat(w.toString(), equalTo("A\nB\nC\nD\n"));
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

    @Test
    public void poppedSectionsEscapeDollars() {
        CodeWriter writer = CodeWriter.createDefault();
        String result = writer.pushState("foo").write("$$Hello").popState().toString();

        assertThat(result, equalTo("$Hello\n"));
    }

    @Test
    public void poppedSectionsEscapeCustomExpressionStarts() {
        CodeWriter writer = CodeWriter.createDefault();
        String result = writer
                .setExpressionStart('#')
                .pushState("foo")
                .write("##Hello")
                .write("$Hello")
                .write("$$Hello")
                .popState()
                .toString();

        assertThat(result, equalTo("#Hello\n$Hello\n$$Hello\n"));
    }

    @Test
    public void canWriteInline() {
        String result = CodeWriter.createDefault()
                .insertTrailingNewline(false)
                .writeInline("foo")
                .writeInline(", bar")
                .toString();

        assertThat(result, equalTo("foo, bar"));
    }

    @Test
    public void writeInlineHandlesSingleNewline() {
        String result = CodeWriter.createDefault()
                .insertTrailingNewline(false)
                .writeInline("foo").indent()
                .writeInline(":\nbar")
                .toString();

        assertThat(result, equalTo("foo:\n    bar"));
    }

    @Test
    public void writeInlineHandlesMultipleNewlines() {
        String result = CodeWriter.createDefault()
                .insertTrailingNewline(false)
                .writeInline("foo:")
                .writeInline(" [").indent()
                .writeInline("\nbar,\nbaz,\nbam,")
                .dedent().writeInline("\n]")
                .toString();

        assertThat(result, equalTo("foo: [\n    bar,\n    baz,\n    bam,\n]"));
    }

    @Test
    public void writeInlineStripsSpaces() {
        String result = CodeWriter.createDefault()
                .insertTrailingNewline(false)
                .trimTrailingSpaces()
                .writeInline("foo ")
                .toString();

        assertThat(result, equalTo("foo"));
    }

    @Test
    public void writeInlineDoesNotAllowIndentationToBeEscaped() {
        String result = CodeWriter.createDefault()
                .setIndentText("\t")
                .insertTrailingNewline(false)
                .indent()
                .indent()
                .writeInline("{foo:")
                .writeInline(" [\n")
                .indent()
                .writeInline("hi,\nbye")
                .dedent()
                .writeInline("\n]\n")
                .dedent()
                .writeInline("}")
                .toString();

        assertThat(result, equalTo("\t\t{foo: [\n\t\t\thi,\n\t\t\tbye\n\t\t]\n\t}"));
    }

    @Test
    public void newlineCanBeDisabled() {
        CodeWriter writer = CodeWriter
                .createDefault()
                .insertTrailingNewline();
        String result = writer
                .disableNewlines()
                .openBlock("[", "]", () -> writer.write("hi"))
                .toString();

        assertThat(result, equalTo("[hi]\n"));
    }

    @Test
    public void newlineCanBeDisabledWithEmptyString() {
        CodeWriter writer = CodeWriter
                .createDefault()
                .insertTrailingNewline();
        String result = writer
                .setNewline("")
                .openBlock("[", "]", () -> writer.write("hi"))
                .enableNewlines()
                .toString();

        assertThat(result, equalTo("[hi]\n"));
    }

    @Test
    public void newlineCanBeMultipleCharacters() {
        CodeWriter writer = CodeWriter
                .createDefault()
                .insertTrailingNewline()
                .setNewline("\r\n");
        String result = writer
                .openBlock("[", "]", () -> writer.write("hi"))
                .enableNewlines()
                .toString();

        assertThat(result, equalTo("[\r\n    hi\r\n]\r\n"));
    }

    @Test
    public void newlineCanBeLotsOfCharacters() {
        CodeWriter writer = CodeWriter
                .createDefault()
                .insertTrailingNewline()
                .setNewline("HELLO_THIS_IS_A_NEWLINE!!!");
        String result = writer
                .write("Hi.")
                .write("There.")
                .toString();

        assertThat(result, equalTo("Hi.HELLO_THIS_IS_A_NEWLINE!!!There.HELLO_THIS_IS_A_NEWLINE!!!"));
    }

    @Test
    public void settingNewlineEnablesNewlines() {
        CodeWriter writer = CodeWriter.createDefault();
        String result = writer
                .disableNewlines()
                .setNewline("\n")
                .openBlock("[", "]", () -> writer.write("hi"))
                .toString();

        assertThat(result, equalTo("[\n    hi\n]\n"));
    }

    @Test
    public void canSetCustomExpressionStartChar() {
        CodeWriter writer = new CodeWriter();
        writer.pushState();
        writer.setExpressionStart('#');
        writer.write("Hi, #L", "1");
        writer.write("Hi, ##L");
        writer.write("Hi, $L");
        writer.write("Hi, $$L");
        writer.popState();
        writer.write("Hi, #L");
        writer.write("Hi, ##L");
        writer.write("Hi, $L", "2");
        writer.write("Hi, $$L");
        String result = writer.toString();

        assertThat(result, equalTo("Hi, 1\n"
                                   + "Hi, #L\n"
                                   + "Hi, $L\n"
                                   + "Hi, $$L\n"
                                   + "Hi, #L\n"
                                   + "Hi, ##L\n"
                                   + "Hi, 2\n"
                                   + "Hi, $L\n"));
    }

    @Test
    public void expressionStartCannotBeSpace() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CodeWriter().setExpressionStart(' '));
    }

    @Test
    public void expressionStartCannotBeNewline() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CodeWriter().setExpressionStart('\n'));
    }

    @Test
    public void canFilterSections() {
        CodeWriter writer = new CodeWriter();
        writer.pushFilteredState(s -> s.toUpperCase(Locale.ENGLISH));
        writer.write("Hello!");
        writer.write("Goodbye!");
        writer.popState();

        assertThat(writer.toString(), equalTo("HELLO!\nGOODBYE!\n"));
    }

    @Test
    public void canComposeSetWithSection() {
        String testSection = "testSection";
        CodeWriter writer = new CodeWriter();

        writer.onSection(testSection, text -> writer.write(text + "1, "));
        writer.onSection(testSection, text -> writer.write(text + "2, "));
        writer.onSection(testSection, text -> writer.write(text + "3"));

        writer.write("[${L@testSection}]", "");

        assertThat(writer.toString(), equalTo("[1, 2, 3]\n"));
    }

    @Test
    public void sectionWithWrite() {
        String testSection = "TEST_SECTION";
        CodeWriter writer = new CodeWriter();

        writer.onSection(testSection, text -> {
            writer.write(text + "addition");
        });

        writer.pushState(testSection);
        writer.popState();

        assertThat(writer.toString(), equalTo("addition\n"));
    }

    @Test
    public void sectionWithWriteInline() {
        String testSection = "TEST_SECTION";
        CodeWriter writer = new CodeWriter();

        writer.onSection(testSection, text -> {
            writer.writeInline(text + "inline addition");
        });

        writer.pushState(testSection);
        writer.popState();

        assertThat(writer.toString(), equalTo("inline addition\n"));
    }

    @Test
    public void canUnwriteMatchingStrings() {
        CodeWriter writer = new CodeWriter().insertTrailingNewline(false);
        writer.writeInline("Hello there");
        writer.unwrite(" there");

        assertThat(writer.toString(), equalTo("Hello"));
    }

    @Test
    public void unwriteDoesNothingWhenNoMatch() {
        CodeWriter writer = new CodeWriter().insertTrailingNewline(false);
        writer.writeInline("Hello there");
        writer.unwrite(" nope");

        assertThat(writer.toString(), equalTo("Hello there"));
    }

    @Test
    public void canUnwriteWhenSubstringTooLong() {
        CodeWriter writer = new CodeWriter().insertTrailingNewline(false);
        writer.writeInline("");
        writer.unwrite("nope");

        assertThat(writer.toString(), equalTo(""));
    }

    @Test
    public void canUnwriteWithTemplates() {
        CodeWriter writer = new CodeWriter().insertTrailingNewline(false);
        writer.writeInline("Hi.Hello");
        writer.unwrite("$L", "Hello");

        assertThat(writer.toString(), equalTo("Hi."));
    }

    @Test
    public void canUnwriteWithTemplatesThatExpandToNothing() {
        CodeWriter writer = new CodeWriter().insertTrailingNewline(false);
        writer.writeInline("Hi.Hello");
        writer.unwrite("$L", "");

        assertThat(writer.toString(), equalTo("Hi.Hello"));
    }

    @Test
    public void formattersOnRootStateWorkOnAllStates() {
        CodeWriter writer = new CodeWriter();
        writer.putFormatter('X', (value, indent) -> value.toString().toUpperCase(Locale.ENGLISH));
        writer.writeInline("$X", "hi");

        writer.pushState();
        writer.writeInline(" $X", "there");
        writer.popState();

        assertThat(writer.toString(), equalTo("HI THERE\n"));
    }

    @Test
    public void formattersArePerState() {
        CodeWriter writer = new CodeWriter();
        // X is uppercase in all states unless overridden.
        writer.putFormatter('X', (value, indent) -> value.toString().toUpperCase(Locale.ENGLISH));
        writer.writeInline("$X", "salutations");

        writer.pushState();
        // Make X lowercase in this state.
        writer.putFormatter('X', (value, indent) -> value.toString().toLowerCase(Locale.ENGLISH));
        // Add Y but only to this state.
        writer.putFormatter('Y', (value, indent) -> value.toString());
        writer.writeInline(" $X $Y", "AND", "gReEtInGs");
        writer.popState();

        // Ensure that X is restored to writing uppercase.
        writer.writeInline("$X", ", friend");

        assertThat(writer.toString(), equalTo("SALUTATIONS and gReEtInGs, FRIEND\n"));
    }

    @Test
    public void canCopySettingsIntoWriter() {
        CodeWriter a = new CodeWriter();
        a.setNewline("\r\n");
        a.setExpressionStart('#');
        a.setIndentText("  ");
        a.setNewlinePrefix(".");
        a.trimTrailingSpaces(true);
        a.trimBlankLines(2);
        a.insertTrailingNewline(false);

        CodeWriter b = new CodeWriter();
        b.copySettingsFrom(a);
        b.indent();

        assertThat(b.getExpressionStart(), equalTo('#'));
        assertThat(b.getNewline(), equalTo("\r\n"));
        assertThat(b.getIndentText(), equalTo("  "));
        assertThat(b.getNewlinePrefix(), equalTo("."));
        assertThat(b.getTrimTrailingSpaces(), equalTo(true));
        assertThat(b.getTrimBlankLines(), equalTo(2));
        assertThat(b.getInsertTrailingNewline(), equalTo(false));
        assertThat(b.getIndentLevel(), equalTo(1));
        assertThat(a.getIndentLevel(), equalTo(0));
    }

    @Test
    public void copyingSettingsDoesNotMutateOtherWriter() {
        CodeWriter a = new CodeWriter();
        CodeWriter b = new CodeWriter();
        b.copySettingsFrom(a);
        b.writeInline("Hello");

        assertThat(b.toString(), equalTo("Hello\n"));
        assertThat(a.toString(), equalTo("\n"));
    }

    @Test
    public void canPassRunnableToFormatters() {
        CodeWriter writer = new CodeWriter();
        writer.write("Hi, $L.", (Runnable) () -> writer.write("TheName"));
        assertThat(writer.toString(), equalTo("Hi, TheName.\n"));
    }

    @Test
    public void canPassRunnableToFormattersAndEvenCreateInlineSections() {
        CodeWriter writer = new CodeWriter();

        writer.onSection("Name", text -> {
            writer.write(text + " (name)");
        });

        writer.write("Hi, $L.", (Runnable) () -> {
            writer.pushState("Name");
            writer.write("TheName");
            writer.popState();
        });

        assertThat(writer.toString(), equalTo("Hi, TheName (name).\n"));
    }

    @Test
    public void canPassRunnableAndKeepTrailingNewline() {
        CodeWriter writer = new CodeWriter();
        writer.write("Hi, $L.", (Runnable) () -> writer.write("TheName\n"));
        assertThat(writer.toString(), equalTo("Hi, TheName\n.\n"));
    }
}

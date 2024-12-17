/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimpleCodeWriterTest {
    @Test
    public void limitsBlankLines() {
        SimpleCodeWriter writer = new SimpleCodeWriter().trimBlankLines().trimTrailingSpaces();
        writer.write("if ($L == \"foo\") {\n\n\n\n", "BAZ")
                .indent()
                .write("print($L)", "BAZ")
                .dedent()
                .write("}");

        assertThat(writer.toString(), equalTo("if (BAZ == \"foo\") {\n\n    print(BAZ)\n}\n"));
    }

    @Test
    public void doesNotLimitBlankLines() {
        SimpleCodeWriter writer = new SimpleCodeWriter().trimTrailingSpaces();
        writer.write("if ($L == \"foo\") {\n\n\n\n", "BAZ")
                .indent()
                .write("print($L)", "BAZ")
                .dedent()
                .write("}");

        assertThat(writer.toString(), equalTo("if (BAZ == \"foo\") {\n\n\n\n\n    print(BAZ)\n}\n"));
    }

    @Test
    public void resetsBlankLineCounterWhenContentAppears() {
        SimpleCodeWriter writer = new SimpleCodeWriter().trimBlankLines().trimTrailingSpaces();
        writer.write(".\n.\n.\n\n.\n\n\n.");

        assertThat(writer.toString(), equalTo(".\n.\n.\n\n.\n\n.\n"));
    }

    @Test
    public void trimsTrailingSpaces() {
        SimpleCodeWriter writer = new SimpleCodeWriter().trimBlankLines().trimTrailingSpaces();
        writer.write("hello there  ");

        assertThat(writer.toString(), equalTo("hello there\n"));
    }

    @Test
    public void toStringCanDisableTrimmingTrailingSpaces() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .insertTrailingNewline(false)
                .trimTrailingSpaces(false)
                .writeInline("hi ");

        assertThat(writer.toString(), equalTo("hi "));
    }

    @Test
    public void trimsSpacesAndBlankLines() {
        SimpleCodeWriter writer = new SimpleCodeWriter().trimTrailingSpaces().trimBlankLines();
        writer.write("hello\n\n\nthere, bud");

        assertThat(writer.toString(), equalTo("hello\n\nthere, bud\n"));
    }

    @Test
    public void insertsTrailingNewlines() {
        SimpleCodeWriter writer = new SimpleCodeWriter().trimTrailingSpaces().trimBlankLines();
        writer.write("hello there, bud");

        assertThat(writer.toString(), equalTo("hello there, bud\n"));
    }

    @Test
    public void trailingNewlineIsAddedToEmptyText() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline();

        assertThat(writer.toString(), equalTo("\n"));
    }

    @Test
    public void canWriteTextWithNewlinePrefixAndBlankLineTrimming() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
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
        SimpleCodeWriter writer = new SimpleCodeWriter();
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
            SimpleCodeWriter writer = new SimpleCodeWriter();
            writer.dedent(10);
        });
    }

    @Test
    public void canDedentToRoot() {
        SimpleCodeWriter writer = new SimpleCodeWriter().indent(10).dedent(-1).write("Hi");

        assertThat(writer.toString(), equalTo("Hi\n"));
    }

    @Test
    public void canIndentDocBlocks() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
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
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("foo");

        assertThat(writer.toString(), equalTo("foo\n"));
    }

    @Test
    public void doesNotInjectNewlineWhenNotNeeded() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("foo");

        assertThat(writer.toString(), equalTo("foo\n"));
    }

    @Test
    public void cannotPopMoreStatesThanExist() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            new SimpleCodeWriter()
                    .pushState()
                    .popState()
                    .popState();
        });
    }

    @Test
    public void canPushAndPopState() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
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
        String result = new SimpleCodeWriter()
                .openBlock("public final class $L {", "Foo")
                .openBlock("public void main(String[] args) {")
                .write("System.out.println(args[0]);")
                .closeBlock("}")
                .closeBlock("}")
                .toString();

        assertThat(result,
                equalTo("public final class Foo {\n    public void main(String[] args) {\n        System.out.println(args[0]);\n    }\n}\n"));
    }

    @Test
    public void doesNotWriteNullOptionally() {
        SimpleCodeWriter w = new SimpleCodeWriter().insertTrailingNewline(false);
        w.writeOptional(null);

        assertThat(w.toString(), equalTo(""));
    }

    @Test
    public void doesNotWriteEmptyOptionals() {
        SimpleCodeWriter w = new SimpleCodeWriter().insertTrailingNewline(false);
        w.writeOptional(Optional.empty());

        assertThat(w.toString(), equalTo(""));
    }

    @Test
    public void doesNotWriteEmptyStrings() {
        SimpleCodeWriter w = new SimpleCodeWriter().insertTrailingNewline(false);
        w.writeOptional("");

        assertThat(w.toString(), equalTo(""));
    }

    @Test
    public void doesNotWriteOptionalsThatContainEmptyStrings() {
        SimpleCodeWriter w = new SimpleCodeWriter().insertTrailingNewline(false);
        w.writeOptional(Optional.of(""));

        assertThat(w.toString(), equalTo(""));
    }

    @Test
    public void writesOptionalsWithNonEmptyStringValues() {
        SimpleCodeWriter w = new SimpleCodeWriter().insertTrailingNewline(false);
        w.writeOptional(Optional.of("hi!"));

        assertThat(w.toString(), equalTo("hi!"));
    }

    @Test
    public void writesLiteralOptionalByUnwrappingIt() {
        SimpleCodeWriter w = new SimpleCodeWriter();
        w.write("$L", Optional.of("hi!"));
        w.write("$L", Optional.empty());
        w.write("$S", Optional.of("hi!"));
        w.write("$S", Optional.empty());

        assertThat(w.toString(), equalTo("hi!\n\n\"hi!\"\n\"\"\n"));
    }

    @Test
    public void formatsNullAsEmptyString() {
        SimpleCodeWriter w = new SimpleCodeWriter();
        w.write("$L", (String) null);
        w.write("$S", (String) null);

        assertThat(w.toString(), equalTo("\n\"\"\n"));
    }

    @Test
    public void writesWithNamedContext() {
        SimpleCodeWriter w = new SimpleCodeWriter()
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
        SimpleCodeWriter w = new SimpleCodeWriter();
        w.putContext("foo", "hello");
        String value = w.getContext("foo", String.class);

        assertThat(value, equalTo("hello"));
    }

    @Test
    public void failsWhenTypedContextDoesNotMatch() {
        SimpleCodeWriter w = new SimpleCodeWriter();
        w.pushState("a");
        w.putContext("foo", "hello");
        w.write("Hello {");

        ClassCastException e = Assertions.assertThrows(ClassCastException.class, () -> {
            w.getContext("foo", Integer.class);
        });

        assertThat(e.getMessage(),
                equalTo("Expected context value 'foo' to be an instance of java.lang.Integer, but "
                        + "found java.lang.String (Debug Info {path=ROOT/a, near=Hello {\\n})"));
    }

    @Test
    public void getsDebugInfoWithNoLines() {
        SimpleCodeWriter w = new SimpleCodeWriter();

        assertThat(w.getDebugInfo().toString(), equalTo("(Debug Info {path=ROOT, near=})"));
    }

    @Test
    public void getsDebugInfoWithNoLinesAndContext() {
        SimpleCodeWriter w = new SimpleCodeWriter();
        w.pushState("a");
        w.pushState("b");

        assertThat(w.getDebugInfo().toString(), equalTo("(Debug Info {path=ROOT/a/b, near=})"));
    }

    @Test
    public void getsDebugInfoWithTwoLines() {
        SimpleCodeWriter w = new SimpleCodeWriter();
        w.write("Hello {");
        w.write("  hello");

        assertThat(w.getDebugInfo().toString(), equalTo("(Debug Info {path=ROOT, near=Hello {\\n  hello\\n})"));
    }

    @Test
    public void getsDebugInfoWithThreeLines() {
        SimpleCodeWriter w = new SimpleCodeWriter();
        w.write("Hello {");
        w.write("  hello1");
        w.write("  hello2");

        assertThat(w.getDebugInfo().toString(), equalTo("(Debug Info {path=ROOT, near=  hello1\\n  hello2\\n})"));
    }

    @Test
    public void hasSections() {
        // Setup the code writer and section interceptors.
        SimpleCodeWriter w = new SimpleCodeWriter().putContext("testing", "123");

        w.onSection("foo", text -> w.write("Yes: $L", text));
        w.onSection("foo", text -> w.write("Si: $L", text));
        w.onSection("placeholder", text -> w.write("$testing:L"));

        // Emit sections with their original values.
        w.pushState("foo").write("Original!").popState();
        w.injectSection(CodeSection.forName("placeholder"));
        w.injectSection(CodeSection.forName("empty-placeholder"));

        assertThat(w.toString(), equalTo("Si: Yes: Original!\n123\n"));
    }

    @Test
    public void allowsForCustomFormatters() {
        // Setup the code writer and section interceptors.
        SimpleCodeWriter w = new SimpleCodeWriter();
        w.putFormatter('X', (text, indent) -> text.toString().replace("\n", "\n" + indent + indent));
        w.setIndentText(" ");
        w.indent(2);
        w.write("Hi.$X.Newline?", "^\n$");

        assertThat(w.toString(), equalTo("  Hi.^\n    $.Newline?\n"));
    }

    @Test
    public void canIntegrateSectionsWithComplexStates() {
        SimpleCodeWriter writer = new SimpleCodeWriter();

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
        writer.writeInline("baz");
        writer.popState();

        writer.pushState("indented").popState();

        writer.popState();
        writer.write(" */");

        assertThat(writer.toString(),
                equalTo(
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
        SimpleCodeWriter writer = new SimpleCodeWriter();

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

        assertThat(writer.toString(),
                equalTo(
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
        SimpleCodeWriter writer = new SimpleCodeWriter();
        String result = writer.openBlock("public {", "}", () -> {
            writer.write("hi();");
        })
                .toString();

        assertThat(result, equalTo("public {\n    hi();\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable1() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        String result = writer.openBlock("public final class $L {", "}", "Foo", () -> {
            writer.openBlock("public void main(String[] args) {", "}", () -> {
                writer.write("System.out.println(args[0]);");
            });
        })
                .toString();

        assertThat(result,
                equalTo("public final class Foo {\n    public void main(String[] args) {\n        System.out.println(args[0]);\n    }\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable2() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        String result = writer.openBlock("public $L $L {", "}", "1", "2", () -> {
            writer.write("hi();");
        })
                .toString();

        assertThat(result, equalTo("public 1 2 {\n    hi();\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable3() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        String result = writer.openBlock("public $L $L $L {", "}", "1", "2", "3", () -> {
            writer.write("hi();");
        })
                .toString();

        assertThat(result, equalTo("public 1 2 3 {\n    hi();\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable4() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        String result = writer.openBlock("public $L $L $L $L {", "}", "1", "2", "3", "4", () -> {
            writer.write("hi();");
        })
                .toString();

        assertThat(result, equalTo("public 1 2 3 4 {\n    hi();\n}\n"));
    }

    @Test
    public void hasOpenBlockRunnable5() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        String result = writer.openBlock("public $L $L $L $L $L {", "}", "1", "2", "3", "4", "5", () -> {
            writer.write("hi();");
        })
                .toString();

        assertThat(result, equalTo("public 1 2 3 4 5 {\n    hi();\n}\n"));
    }

    @Test
    public void poppedSectionsEscapeDollars() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        String result = writer.pushState("foo").write("$$Hello").popState().toString();

        assertThat(result, equalTo("$Hello\n"));
    }

    @Test
    public void poppedSectionsEscapeCustomExpressionStarts() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
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
        String result = new SimpleCodeWriter()
                .insertTrailingNewline(false)
                .writeInline("foo")
                .writeInline(", bar")
                .toString();

        assertThat(result, equalTo("foo, bar"));
    }

    @Test
    public void writeInlineHandlesSingleNewline() {
        String result = new SimpleCodeWriter()
                .insertTrailingNewline(false)
                .writeInline("foo")
                .indent()
                .writeInline(":\nbar")
                .toString();

        assertThat(result, equalTo("foo:\n    bar"));
    }

    @Test
    public void writeInlineHandlesMultipleNewlines() {
        String result = new SimpleCodeWriter()
                .insertTrailingNewline(false)
                .writeInline("foo:")
                .writeInline(" [")
                .indent()
                .writeInline("\nbar,\nbaz,\nbam,")
                .dedent()
                .writeInline("\n]")
                .toString();

        assertThat(result, equalTo("foo: [\n    bar,\n    baz,\n    bam,\n]"));
    }

    @Test
    public void writeInlineStripsSpaces() {
        String result = new SimpleCodeWriter()
                .insertTrailingNewline(false)
                .trimTrailingSpaces()
                .writeInline("foo ")
                .toString();

        assertThat(result, equalTo("foo"));
    }

    @Test
    public void writeInlineDoesNotAllowIndentationToBeEscaped() {
        String result = new SimpleCodeWriter()
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
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .insertTrailingNewline();
        String result = writer
                .disableNewlines()
                .openBlock("[", "]", () -> writer.write("hi"))
                .toString();

        assertThat(result, equalTo("[hi]\n"));
    }

    @Test
    public void newlineCanBeDisabledWithEmptyString() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
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
        SimpleCodeWriter writer = new SimpleCodeWriter()
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
        SimpleCodeWriter writer = new SimpleCodeWriter()
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
        SimpleCodeWriter writer = new SimpleCodeWriter();
        String result = writer
                .disableNewlines()
                .setNewline("\n")
                .openBlock("[", "]", () -> writer.write("hi"))
                .toString();

        assertThat(result, equalTo("[\n    hi\n]\n"));
    }

    @Test
    public void canSetCustomExpressionStartChar() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
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

        assertThat(result,
                equalTo("Hi, 1\n"
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
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SimpleCodeWriter().setExpressionStart(' '));
    }

    @Test
    public void expressionStartCannotBeNewline() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SimpleCodeWriter().setExpressionStart('\n'));
    }

    @Test
    public void canFilterSections() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.pushFilteredState(s -> s.toUpperCase(Locale.ENGLISH));
        writer.write("Hello!");
        writer.write("Goodbye!");
        writer.popState();

        assertThat(writer.toString(), equalTo("HELLO!\nGOODBYE!\n"));
    }

    @Test
    public void canFilterSectionsWithInterceptorsOutsideState() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.onSection("foo", text -> {
            writer.writeWithNoFormatting(text + "??");
        });
        writer.pushState("foo");
        writer.pushFilteredState(s -> s.toUpperCase(Locale.ENGLISH));
        writer.write("Hello!");
        writer.writeInline("Goodbye!");
        writer.popState();
        writer.popState();

        assertThat(writer.toString(), equalTo("HELLO!\nGOODBYE!??\n"));
    }

    @Test
    public void canFilterSectionsWithInterceptorsInsideState() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.pushState("foo");
        writer.onSection("foo", text -> {
            writer.writeWithNoFormatting(text + "??");
        });
        writer.pushFilteredState(s -> s.toUpperCase(Locale.ENGLISH));
        writer.write("Hello!");
        writer.writeInline("Goodbye!");
        writer.popState();
        writer.popState();

        assertThat(writer.toString(), equalTo("HELLO!\nGOODBYE!??\n"));
    }

    @Test
    public void canComposeSetWithSection() {
        String testSection = "testSection";
        SimpleCodeWriter writer = new SimpleCodeWriter();

        writer.onSection(testSection, text -> writer.writeInline(text + "1, "));
        writer.onSection(testSection, text -> writer.writeInline(text + "2, "));
        writer.onSection(testSection, text -> writer.writeInline(text + "3"));

        writer.write("[${L@testSection}]", "");

        assertThat(writer.toString(), equalTo("[1, 2, 3]\n"));
    }

    @Test
    public void sectionWithWrite() {
        String testSection = "TEST_SECTION";
        SimpleCodeWriter writer = new SimpleCodeWriter();

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
        SimpleCodeWriter writer = new SimpleCodeWriter();

        writer.onSection(testSection, text -> {
            writer.writeInline(text + "inline addition");
        });

        writer.pushState(testSection);
        writer.popState();

        assertThat(writer.toString(), equalTo("inline addition\n"));
    }

    @Test
    public void sectionWithSucceedingIndentedWrite() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.writeInline("ori");
        writer.pushState();
        writer.write("ginal");
        writer.popState();
        writer.setNewlinePrefix("/// ");
        writer.write("after");

        assertThat(writer.toString(), equalTo("original\n/// after\n"));
    }

    @Test
    public void namedSectionWithSucceedingIndentedWrite() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.writeInline("ori");
        writer.pushState("named");
        writer.write("ginal");
        writer.popState();
        writer.setNewlinePrefix("/// ");
        writer.write("after");

        assertThat(writer.toString(), equalTo("original\n/// after\n"));
    }

    @Test
    public void canUnwriteMatchingStrings() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.writeInline("Hello there");
        writer.unwrite(" there");

        assertThat(writer.toString(), equalTo("Hello"));
    }

    @Test
    public void unwriteDoesNothingWhenNoMatch() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.writeInline("Hello there");
        writer.unwrite(" nope");

        assertThat(writer.toString(), equalTo("Hello there"));
    }

    @Test
    public void canUnwriteWhenSubstringTooLong() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.writeInline("");
        writer.unwrite("nope");

        assertThat(writer.toString(), equalTo(""));
    }

    @Test
    public void canUnwriteWithTemplates() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.writeInline("Hi.Hello");
        writer.unwrite("$L", "Hello");

        assertThat(writer.toString(), equalTo("Hi."));
    }

    @Test
    public void canUnwriteWithTemplatesThatExpandToNothing() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.writeInline("Hi.Hello");
        writer.unwrite("$L", "");

        assertThat(writer.toString(), equalTo("Hi.Hello"));
    }

    @Test
    public void formattersOnRootStateWorkOnAllStates() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.putFormatter('X', (value, indent) -> value.toString().toUpperCase(Locale.ENGLISH));
        writer.writeInline("$X", "hi");

        writer.pushState();
        writer.writeInline(" $X", "there");
        writer.popState();

        assertThat(writer.toString(), equalTo("HI THERE\n"));
    }

    @Test
    public void formattersArePerState() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
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
        SimpleCodeWriter a = new SimpleCodeWriter();
        a.setNewline("\r\n");
        a.setExpressionStart('#');
        a.setIndentText("  ");
        a.setNewlinePrefix(".");
        a.trimTrailingSpaces(true);
        a.trimBlankLines(2);
        a.insertTrailingNewline(false);

        SimpleCodeWriter b = new SimpleCodeWriter();
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
        SimpleCodeWriter a = new SimpleCodeWriter();
        SimpleCodeWriter b = new SimpleCodeWriter();
        b.copySettingsFrom(a);
        b.writeInline("Hello");

        assertThat(b.toString(), equalTo("Hello\n"));
        assertThat(a.toString(), equalTo("\n"));
    }

    @Test
    public void canPassRunnableToFormatters() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("Hi, $C.", (Runnable) () -> writer.writeInline("TheName"));
        assertThat(writer.toString(), equalTo("Hi, TheName.\n"));
    }

    // This behavior completely removes the need for inline section syntax.
    @Test
    public void canPassRunnableToFormattersAndEvenCreateInlineSections() {
        SimpleCodeWriter writer = new SimpleCodeWriter();

        writer.onSection("Name", text -> {
            writer.writeInline("$L (name)", text);
        });

        writer.write("Hi, $C.", (Runnable) () -> {
            writer.pushState("Name");
            writer.writeInline("TheName");
            writer.popState();
        });

        assertThat(writer.toString(), equalTo("Hi, TheName (name).\n"));
    }

    @Test
    public void canPassRunnableAndKeepTrailingNewline() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("Hi, $C.", (Runnable) () -> writer.write("TheName\n"));
        assertThat(writer.toString(), equalTo("Hi, TheName\n.\n"));
    }

    @Test
    public void canPassRunnableAndByDefaultDoesNotKeepTrailingNewline() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("Hi, $C.", (Runnable) () -> writer.write("TheName"));
        assertThat(writer.toString(), equalTo("Hi, TheName.\n"));
    }

    @Test
    public void canCreateTypedSections() {
        MyWriter writer = new MyWriter();

        // When a section of type MyPojo is encountered, intercept it.
        writer.onSection(new CodeInterceptor<MyPojo, MyWriter>() {
            @Override
            public Class<MyPojo> sectionType() {
                return MyPojo.class;
            }

            @Override
            public void write(MyWriter writer, String previousText, MyPojo section) {
                if (section.name.equals("Thomas")) {
                    section.count++;
                    writer.write("Hi, Thomas!");
                }
                writer.writeInlineWithNoFormatting(previousText);
            }
        });

        // Create a custom typed section value.
        MyPojo myPojo = new MyPojo("Thomas", 0);

        writer.pushState(myPojo);
        writer.write("How are you?");
        writer.popState(); // At this point, intercept the section.

        assertThat(myPojo.count, equalTo(1));
        assertThat(writer.toString(), equalTo("Hi, Thomas!\nHow are you?\n"));
    }

    private static final class MyWriter extends AbstractCodeWriter<MyWriter> {}

    private static final class MyPojo implements CodeSection {
        public int count = 0;
        public String name = "";

        MyPojo(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    @Test
    public void canAppendToTypedSections() {
        MyWriter writer = new MyWriter();
        MyPojo myPojo = new MyPojo("Thomas", 0);
        writer.pushState(myPojo);

        writer.onSection(CodeInterceptor.appender(MyPojo.class, (w, section) -> {
            if (section.name.equals("Thomas")) {
                section.count++;
                w.write("Hi, Thomas!");
            }
        }));

        writer.write("How are you?");
        writer.popState();

        assertThat(myPojo.count, equalTo(1));
        assertThat(writer.toString(), equalTo("How are you?\nHi, Thomas!\n"));
    }

    @Test
    public void canPrependToTypedSections() {
        MyWriter writer = new MyWriter();
        MyPojo myPojo = new MyPojo("Thomas", 0);
        writer.pushState(myPojo);

        writer.onSection(new CodeInterceptor.Prepender<MyPojo, MyWriter>() {
            @Override
            public Class<MyPojo> sectionType() {
                return MyPojo.class;
            }

            @Override
            public void prepend(MyWriter writer, MyPojo section) {
                if (section.name.equals("Thomas")) {
                    section.count++;
                    writer.write("Hi, Thomas!");
                }
            }
        });

        writer.write("How are you?");
        writer.popState();

        assertThat(myPojo.count, equalTo(1));
        assertThat(writer.toString(), equalTo("Hi, Thomas!\nHow are you?\n"));
    }

    // Section interceptors are executed after popping state, which means they
    // don't use the setting configured for the popped state, including any
    // context variables specific to the state that was just popped. They
    // map over a state, but aren't part of it.
    @Test
    public void canAccessOnlyOuterStateVariablesInPopState() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.putContext("baz", 1);

        writer.pushState("foo");
        writer.putContext("baz", 2);
        writer.onSection("foo", text -> {
            writer.write(writer.getContext("baz"));
        });
        writer.popState();

        assertThat(writer.toString(), equalTo("1\n"));
    }

    @Test
    public void injectSectionProvidesShorterWayToAddSectionHooks() {
        MyWriter writer = new MyWriter();
        writer.onSection(CodeInterceptor.appender(MyPojo.class, (w, section) -> w.write("$L", section.name)));
        writer.onSection(CodeInterceptor.appender(MyPojo.class, (w, section) -> w.write("Foo")));
        writer.onSection(CodeInterceptor.appender(MyPojo.class, (w, section) -> w.write("Bar")));
        writer.write("Name?");
        writer.injectSection(new MyPojo("Thomas", 0));

        assertThat(writer.toString(), equalTo("Name?\nThomas\nFoo\nBar\n"));
    }

    // This test ensures that infinite recursion isn't caused when an interceptor is
    // created that intercepts a CodeSection instances without filtering based on name.
    // This is prevented by using "AnonymousCodeSection"s internally within
    // AbstractTypedCodeWriter.
    @Test
    public void injectsInlineSectionsThatWriteInlineWithoutInfiniteRecursion() {
        MyWriter writer = new MyWriter();
        writer.onSection(CodeInterceptor.appender(CodeSection.class, (w, section) -> w.write("DROP_TABLE1,")));
        writer.onSection(CodeInterceptor.appender(CodeSection.class, (w, section) -> w.write("DROP_TABLE2,")));
        writer.onSection(CodeInterceptor.appender(CodeSection.class, (w, section) -> w.write("DROP_TABLE3,")));
        writer.write("Name: ${L@foo|}", "");

        assertThat(writer.toString(),
                equalTo("Name: DROP_TABLE1,\n"
                        + "      DROP_TABLE2,\n"
                        + "      DROP_TABLE3,\n"));
    }

    @Test
    public void canWriteInlineSectionsWithNoNewlines() {
        MyWriter writer = new MyWriter();
        writer.onSection(CodeInterceptor.appender(CodeSection.class, (w, section) -> w.writeInline("DROP_TABLE1,")));
        writer.onSection(CodeInterceptor.appender(CodeSection.class, (w, section) -> w.writeInline("DROP_TABLE2,")));
        writer.onSection(CodeInterceptor.appender(CodeSection.class, (w, section) -> w.writeInline("DROP_TABLE3,")));
        writer.onSection(CodeInterceptor.appender(CodeSection.class, (w, section) -> w.unwrite(",")));
        writer.write("Name: ${L@foo}", "");

        assertThat(writer.toString(), equalTo("Name: DROP_TABLE1,DROP_TABLE2,DROP_TABLE3\n"));
    }

    @Test
    public void injectsEmptySections() {
        MyWriter writer = new MyWriter().insertTrailingNewline(false);
        writer.injectSection(new MyPojo("Thomas", 0));

        assertThat(writer.toString(), equalTo(""));
    }

    @Test
    public void injectsSingleSectionContent() {
        MyWriter writer = new MyWriter();
        writer.onSection(CodeInterceptor.appender(MyPojo.class, (w, section) -> w.write(section.name)));
        writer.injectSection(new MyPojo("Name", 0));

        assertThat(writer.toString(), equalTo("Name\n"));
    }

    @Test
    public void ensuresNewlineIsPresent() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.writeInline("Foo");
        writer.ensureNewline();
        writer.writeInline("Bar");
        writer.ensureNewline();
        writer.write("Baz");
        writer.ensureNewline();
        writer.write("Bam");

        assertThat(writer.toString(), equalTo("Foo\nBar\nBaz\nBam\n"));
    }
}

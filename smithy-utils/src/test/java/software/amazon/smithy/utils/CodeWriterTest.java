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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

        assertThat(e.getMessage(), equalTo("Expected context value 'foo' to be an instance of java.lang.Integer, but "
                                           + "found java.lang.String (Debug Info {path=ROOT/a, near=Hello {\\n})"));
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
    public void canPrependAndAppendToSection() {
        CodeWriter w = CodeWriter.createDefault();
        w.onSectionPrepend("foo", () -> w.write("A"));
        w.onSection("foo", text -> {
            w.writeWithNoFormatting(text);
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
        writer.writeInline("baz");
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
    public void canFilterSectionsWithInterceptorsOutsideState() {
        CodeWriter writer = new CodeWriter();
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
        CodeWriter writer = new CodeWriter();
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
        CodeWriter writer = new CodeWriter();

        writer.onSection(testSection, text -> writer.writeInline(text + "1, "));
        writer.onSection(testSection, text -> writer.writeInline(text + "2, "));
        writer.onSection(testSection, text -> writer.writeInline(text + "3"));

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
    public void sectionWithSucceedingIndentedWrite() {
        CodeWriter writer = new CodeWriter();
        writer.writeInline("ori");
        writer.pushState();
        writer.write("ginal");
        writer.popState();
        writer.setNewlinePrefix("/// ");
        writer.write("after");

        assertThat(writer.toString(), equalTo("original\n/// after\n"));
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
    public void flattensStatesOfOtherWhenCopying() {
        CodeWriter a = new CodeWriter();
        a.pushState();
        a.putFormatter('A', (value, indent) -> value.toString().toUpperCase(Locale.ENGLISH));
        a.putContext("a", 1);
        a.putContext("b", "B");
        a.pushState();
        a.putContext("b", 2); // override "b"
        a.putFormatter('B', (value, indent) -> value.toString().toLowerCase(Locale.ENGLISH));

        CodeWriter b = new CodeWriter();
        b.putContext("c", 3);
        b.copySettingsFrom(a);
        b.writeInline("${1A} ${2B} ${a:L} ${b:L} ${c:L}", "Hello", "Goodbye");

        assertThat(b.toString(), equalTo("HELLO goodbye 1 2 3\n"));
        assertThat(a.toString(), equalTo("\n"));
    }

    @Test
    public void canPassRunnableToFormatters() {
        CodeWriter writer = new CodeWriter();
        writer.write("Hi, $C.", (Runnable) () -> writer.writeInline("TheName"));
        assertThat(writer.toString(), equalTo("Hi, TheName.\n"));
    }

    // This behavior completely removes the need for inline section syntax.
    @Test
    public void canPassRunnableToFormattersAndEvenCreateInlineSections() {
        CodeWriter writer = new CodeWriter();

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
        CodeWriter writer = new CodeWriter();
        writer.write("Hi, $C.", (Runnable) () -> writer.write("TheName\n"));
        assertThat(writer.toString(), equalTo("Hi, TheName\n.\n"));
    }

    @Test
    public void canPassRunnableAndByDefaultDoesNotKeepTrailingNewline() {
        CodeWriter writer = new CodeWriter();
        writer.write("Hi, $C.", (Runnable) () -> writer.write("TheName"));
        assertThat(writer.toString(), equalTo("Hi, TheName.\n"));
    }

    // Section interceptors are executed after popping state, which means they
    // don't use the setting configured for the popped state, including any
    // context variables specific to the state that was just popped. They
    // map over a state, but aren't part of it.
    @Test
    public void canAccessOnlyOuterStateVariablesInPopState() {
        CodeWriter writer = new CodeWriter();
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
    public void ensuresNewlineIsPresent() {
        CodeWriter writer = new CodeWriter();
        writer.writeInline("Foo");
        writer.ensureNewline();
        writer.writeInline("Bar");
        writer.ensureNewline();
        writer.write("Baz");
        writer.ensureNewline();
        writer.write("Bam");

        assertThat(writer.toString(), equalTo("Foo\nBar\nBaz\nBam\n"));
    }

    // Dynamically creating builders for a section doesn't work when pushing
    // sections into captured sections that haven't written anything yet.
    // It ends up swallowing the text written to sub-sections because the
    // top-level section's builder wasn't written yet. This test makes sure
    // that doesn't happen.
    @Test
    public void alwaysWritesToParentBuilders() {
        SimpleCodeWriter writer = new SimpleCodeWriter();

        writer.pushState("Hi");
        writer.pushState();
        writer.write("Hello");
        writer.popState();
        writer.popState();

        assertThat(writer.toString(), equalTo("Hello\n"));
    }

    @Test
    public void skipsConditionalsWithAllWhitespaceLines1() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.putContext("foo", true);
        writer.write(" ${?foo}\n" // "  " is skipped.
                     + "  ${foo:L} foo ${foo:L}\n" // "  " is kept, so is the newline.
                     + " ${/foo}\n" // whole line is skipped.
                     + " ${^foo}\n" // whole line is skipped.
                     + "  not ${foo:L}\n" // skipped.
                     + " ${/foo}\n" // Whole line is skipped.
                     + "Who?"); // Includes only "Who?"

        assertThat(writer.toString(), equalTo("  true foo true\nWho?"));
    }

    @Test
    public void skipsConditionalsWithAllWhitespaceLines2() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.putContext("foo", true);
        writer.write("${?foo}\n" // "  " is skipped.
                     + " ${foo:L}\n" // " " is kept, so is the newline.
                     + "${/foo}\n" // whole line is skipped.
                     + "${^foo}\n" // whole line is skipped.
                     + " not ${foo:L}\n" // skipped.
                     + "${/foo}\n" // Whole line is skipped.
                     + "Who?"); // Includes only "Who?"

        assertThat(writer.toString(), equalTo(" true\nWho?"));
    }

    @Test
    public void skipsConditionalsWithAllWhitespaceLines3() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.write(" ${?foo}\n" // Whole line is skipped.
                     + "  ${foo:L} foo ${foo:L}\n" // Whole line is skipped.
                     + " ${/foo}\n" // whole line is skipped.
                     + " ${^foo}\n" // whole line is skipped.
                     + "  not ${foo:L}\n" // Includes "  not \n"
                     + " ${/foo}\n" // Whole line is skipped.
                     + "Who?"); // Includes only "Who?"

        assertThat(writer.toString(), equalTo("  not \nWho?"));
    }

    @Test
    public void expandsEmptyConditionalSections() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.write("Hi\n"
                     + "${^foo}\n"
                     + "${/foo}");

        assertThat(writer.toString(), equalTo("Hi\n"));
    }

    // This test is important because formatters are applied only if the
    // condition they're contained within evaluate to true. This means that
    // error handling is deferred until evaluation.
    @Test
    public void failsWhenFormatterIsUnknownInsideConditional() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);

        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            writer.write("${^foo}$P${/foo}", 10);
        });

        assertThat(e.getMessage(), equalTo("Syntax error at line 1 column 9: Unknown formatter `P` "
                                           + "found in format string (template: ${^foo}$P${/foo}) "
                                           + "(Debug Info {path=ROOT, near=})"));
    }

    // This test is important because formatters are applied only if the
    // condition they're contained within evaluate to true. This means that
    // error handling is deferred until evaluation.
    @Test
    public void ignoresInvalidFormattersInUnevaluatedConditions() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.write("${?foo}$P${/foo}", 10);

        assertThat(writer.toString(), equalTo(""));
    }

    @ParameterizedTest
    @MethodSource("nestedBlocksProvider")
    public void evaluatesNestedBlocks(String line) {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.write("${^foo}" + line
                     + " ${^foo}Hi1${/foo}" + line
                     + " ${^foo}${^foo}Hi2${?foo}No${/foo}${/foo}${/foo}" + line
                     + " ${^foo}Hi3${/foo}" + line
                     + "${/foo}");

        assertThat(writer.toString(), equalTo(" Hi1" + line + " Hi2" + line + " Hi3" + line));
    }

    public static Stream<Arguments> nestedBlocksProvider() {
        return Stream.of(Arguments.of("\r"), Arguments.of("\n"), Arguments.of("\r\n"));
    }

    @Test
    public void canIterateOverVariable() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.putContext("foo", Arrays.asList("a", "b", "c"));
        writer.write("${#foo}\n"
                     + " - ${key:L}: ${value:L}\n"
                     + "${/foo}");

        assertThat(writer.toString(), equalTo(" - 0: a\n - 1: b\n - 2: c\n"));
    }

    @Test
    public void canIterateOverVariableAndDelayConditionals() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.putContext("foo", Arrays.asList("a", "", "b"));
        writer.write("${#foo}\n"
                     + " ${?value}\n"
                     + " - ${key:L}: ${value:L}\n"
                     + " ${/value}\n"
                     + "${/foo}\n");

        assertThat(writer.toString(), equalTo(" - 0: a\n - 2: b\n"));
    }

    @Test
    public void canGetFirstAndLastFromIterator() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.putContext("foo", Arrays.asList("a", "b", "c"));
        writer.write("${#foo}\n"
                     + "${?key.first}\n"
                     + "[\n"
                     + "${/key.first}\n"
                     + "    ${value:L}${^key.last},${/key.last}\n"
                     + "${?key.last}\n"
                     + "]\n"
                     + "${/key.last}\n"
                     + "${/foo}\n");

        assertThat(writer.toString(), equalTo("[\n    a,\n    b,\n    c\n]\n"));
    }

    @Test
    public void canGetFirstAndLastFromIteratorWithCustomName() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.putContext("foo", Arrays.asList("a", "b", "c"));
        writer.write("${#foo as i, value}\n"
                     + "${?i.first}\n"
                     + "[\n"
                     + "${/i.first}\n"
                     + "    ${value:L}${^i.last},${/i.last}\n"
                     + "${?i.last}\n"
                     + "]\n"
                     + "${/i.last}\n"
                     + "${/foo}\n");

        assertThat(writer.toString(), equalTo("[\n    a,\n    b,\n    c\n]\n"));
    }

    @ParameterizedTest
    @MethodSource("truthyAndFalseyTestCases")
    public void truthyAndFalsey(Object fooValue, String expected) {
        String template = "${?foo}A${/foo}${^foo}B${/foo}";
        String actual = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false)
                .putContext("foo", fooValue)
                .write(template)
                .toString();

        assertThat(actual, equalTo(expected));
    }

    public static Stream<Arguments> truthyAndFalseyTestCases() {
        return Stream.of(
                Arguments.of(null, "B"),
                Arguments.of(false, "B"),
                Arguments.of("", "B"),
                Arguments.of(Collections.emptyList(), "B"),
                Arguments.of(Collections.emptySet(), "B"),
                Arguments.of(Collections.emptyMap(), "B"),
                Arguments.of(Optional.empty(), "B"),
                Arguments.of(0, "A"),
                Arguments.of(1, "A"),
                Arguments.of("a", "A"),
                Arguments.of(ListUtils.of("a"), "A"),
                Arguments.of(SetUtils.of("a"), "A"),
                Arguments.of(MapUtils.of("a", "a"), "A"),
                Arguments.of(Optional.of(0), "A"),
                Arguments.of(Optional.of(Collections.emptyList()), "A")
        );
    }

    @ParameterizedTest
    @MethodSource("iterationTestCases")
    public void handlesIteration(Object fooValue, String expected) {
        String template = "${#foo}\n"
                          + "k: ${key:L}, v: ${value:L}, f: ${key.first:L}, l: ${key.last:L}\n"
                          + "${/foo}";
        String actual = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false)
                .putContext("foo", fooValue)
                .write(template)
                .toString();

        assertThat(actual, equalTo(expected));
    }

    public static Stream<Arguments> iterationTestCases() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of(false, ""),
                Arguments.of("", ""),
                Arguments.of(Collections.emptyList(), ""),
                Arguments.of(Collections.emptySet(), ""),
                Arguments.of(Collections.emptyMap(), ""),
                Arguments.of(Optional.empty(), ""),
                Arguments.of(Optional.of(0), ""),
                Arguments.of(Optional.of(Collections.emptyList()), ""),
                Arguments.of(ListUtils.of("a"), "k: 0, v: a, f: true, l: true\n"),
                Arguments.of(SetUtils.of("a"), "k: 0, v: a, f: true, l: true\n"),
                Arguments.of(MapUtils.of("ak", "av"), "k: ak, v: av, f: true, l: true\n"),
                Arguments.of(Arrays.asList("a", "b"),
                             "k: 0, v: a, f: true, l: false\nk: 1, v: b, f: false, l: true\n")
        );
    }

    @Test
    public void detectsOutOfOrderClosingBlocks() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            new SimpleCodeWriter().write("${?foo}${?bar}${/foo}${/bar}");
        });

        assertThat(e.getMessage(), containsString("Invalid closing tag: 'foo'. Expected: 'bar'"));
    }

    @Test
    public void detectsClosingUnopenedBlock() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            new SimpleCodeWriter().write("${/bar}");
        });

        assertThat(e.getMessage(), containsString("Attempted to close unopened tag: 'bar'"));
    }

    @Test
    public void detectsUnclosedBlocks() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            new SimpleCodeWriter().write("${?bar}${?foo}");
        });

        assertThat(e.getMessage(), equalTo("Unclosed parse conditional blocks: [foo, bar]"));
    }

    @Test
    public void handlesNestedBlockAlignment() {
        SimpleCodeWriter writer = new SimpleCodeWriter()
                .trimTrailingSpaces(false)
                .insertTrailingNewline(false);
        writer.onSection("nested", writer::writeInlineWithNoFormatting);
        writer.write("Hello: ${C@nested|}",
                     writer.consumer(w -> w.write(". ${C|}",
                                                  w.consumer(w2 -> w2.write("* Hi\n* There")))));
        String actual = writer.toString();

        assertThat(actual, equalTo("Hello: . * Hi\n"
                                   + "         * There"));
    }

    @Test
    public void writesStackTraceInfo() {
        MyCustomWriter writer = new MyCustomWriter();
        writer.enableStackTraceComments(true);

        writer.writeWithNoFormatting("Hello 1");
        writer.write("Hello 2");
        writer.indent().write("Hello 3");
        writer.writeInline("Hello 4");
        writer.writeInline("Hello 5");
        String[] result = writer.toString().split("\n");

        assertThat(result[0], startsWith("/* software.amazon.smithy.utils.CodeWriterTest.writesStackTraceInfo"));
        assertThat(result[0], endsWith("*/ Hello 1"));
        assertThat(result[1], startsWith("/* software.amazon.smithy.utils.CodeWriterTest.writesStackTraceInfo"));
        assertThat(result[1], endsWith("*/ Hello 2"));
        assertThat(result[2], startsWith("    /* software.amazon.smithy.utils.CodeWriterTest.writesStackTraceInfo"));
        assertThat(result[2], endsWith("*/ Hello 3"));
        assertThat(result[3], startsWith("    /* software.amazon.smithy.utils.CodeWriterTest.writesStackTraceInfo"));
        assertThat(result[3], containsString("Hello 4"));
        assertThat(result[3], endsWith("*/ Hello 5"));
    }

    @Test
    public void writesStackTraceInfoIgnoringInlineWrites() {
        // This writer ignores inline writes and puts the comment on the line before.
        MyCustomPythonWriter writer = new MyCustomPythonWriter();
        writer.enableStackTraceComments(true);

        writer.writeWithNoFormatting("Hello 1");
        writer.write("Hello 2");
        writer.indent().write("Hello 3");
        writer.writeInline("Hello 4|");
        writer.writeInline("Hello 5");
        String[] result = writer.toString().split("\n");

        assertThat(result[0], startsWith("# software.amazon.smithy.utils.CodeWriterTest.writesStackTraceInfo"));
        assertThat(result[1], equalTo("Hello 1"));
        assertThat(result[2], startsWith("# software.amazon.smithy.utils.CodeWriterTest.writesStackTraceInfo"));
        assertThat(result[3], equalTo("Hello 2"));
        assertThat(result[4], startsWith("    # software.amazon.smithy.utils.CodeWriterTest.writesStackTraceInfo"));
        assertThat(result[5], equalTo("    Hello 3"));
        assertThat(result[6], equalTo("    Hello 4|Hello 5"));
    }

    @Test
    public void filteringAllStackFramesEmitsNoStackComment() {
        // This writer ignores inline writes and puts the comment on the line before.
        MyCustomFilteredWriter writer = new MyCustomFilteredWriter();
        writer.enableStackTraceComments(true);
        writer.write("Hello");

        assertThat(writer.toString(), equalTo("Hello\n"));
    }

    @Test
    public void canAccessCodeSectionGettersFromTemplates() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.pushState(new MySection());
        writer.write("${foo:L}: ${ten:L}... ${nope:L}.");
        writer.popState();

        assertThat(writer.toString(), equalTo("foo: 10... .\n"));
    }

    @Test
    public void providesContextWhenBadGetterIsCalled() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = new SimpleCodeWriter();
            writer.pushState(new MySection());
            writer.write("${bad:L}");
        });

        assertThat(e.getMessage(), containsString("Unable to get context 'bad' from a matching method of the current "
                                                  + "CodeSection: This was bad! "));
        // The debug info contains the class name of the section.
        assertThat(e.getMessage(), containsString(MySection.class.getCanonicalName()));
    }

    @Test
    public void namedContextValuesOverrideSectionGetters() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.pushState(new MySection());
        writer.putContext("bad", "ok actually");
        writer.write("${foo:L}: ${bad:L}");
        writer.popState();

        assertThat(writer.toString(), equalTo("foo: ok actually\n"));
    }

    @Test
    public void canAccessStateClassesOfInheritedStates() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.putContext("a", "A");
        writer.pushState();
        writer.pushState(new CustomSection());
        writer.putContext("b", "B");
        writer.pushState();
        writer.write("${a:L}, ${b:L}, ${hello:L}${partial:C|}");
        writer.popState();

        assertThat(writer.toString(), equalTo("A, B, Hello, 0T, 1T, 2T\n"));
    }

    @Test
    public void canRemoveContextFromTheCurrentState() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.putContext("a", "A");
        writer.pushState();
        writer.putContext("b", "B");
        writer.removeContext("a");

        writer.write("${a:L}, ${b:L}");
        writer.popState();

        assertThat(writer.toString(), equalTo(", B\n"));
    }

    private static final class MySection implements CodeSection {
        public String getFoo() {
            return "foo";
        }

        public int getTen() {
            return 10;
        }

        public String bad() {
            throw new RuntimeException("This was bad!");
        }
    }

    private static final class MyCustomWriter extends AbstractCodeWriter<MyCustomWriter> {
        // Ensure that subclass methods are automatically filtered out as irrelevant frames.
        @Override
        public MyCustomWriter write(Object content, Object... args) {
            return super.write(content, args);
        }

        // Ensure that subclass methods are automatically filtered out as irrelevant frames.
        @Override
        public MyCustomWriter writeInline(Object content, Object... args) {
            return super.writeInline(content, args);
        }
    }

    private static final class MyCustomPythonWriter extends AbstractCodeWriter<MyCustomPythonWriter> {
        @Override
        protected String formatWithStackTraceElement(String content, StackTraceElement element, boolean inline) {
            if (inline) {
                return content;
            } else {
                return "# " + element + getNewline() + content;
            }
        }
    }

    private static final class MyCustomFilteredWriter extends AbstractCodeWriter<MyCustomFilteredWriter> {
        @Override
        protected boolean isStackTraceRelevant(StackTraceElement e) {
            return false;
        }
    }

    public static final class CustomSection implements CodeSection {
        public String hello() {
            return "Hello";
        }

        public List<String> values() {
            return ListUtils.of("a", "b", "c");
        }

        public String test() {
            return "T";
        }

        public Consumer<AbstractCodeWriter<?>> partial() {
            // This partial ensures that nested contexts can reach up to parent contexts and sections to get values.
            return writer -> {
                writer.write("${#values}, ${key:L}${test:L}${/values}");
            };
        }
    }
}

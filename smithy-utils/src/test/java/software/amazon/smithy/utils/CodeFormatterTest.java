/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CodeFormatterTest {

    private SimpleCodeWriter createWriter() {
        return new SimpleCodeWriter();
    }

    private static String valueOf(Object value, String indent) {
        return String.valueOf(value);
    }

    @Test
    public void formatsDollarLiterals() {
        SimpleCodeWriter writer = createWriter();
        String result = writer.format("hello $$.");

        assertThat(result, equalTo("hello $."));
    }

    @Test
    public void formatsRelativeLiterals() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello $L", "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void formatsRelativeLiteralsInBraces() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello ${L}", "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void requiresTextAfterOpeningBrace() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.format("hello ${", "there");
        });

        assertThat(e.getMessage(), containsString("expected one of the following tokens: '!'"));
    }

    @Test
    public void requiresBraceIsClosed() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello ${L .", "there");
        });
    }

    @Test
    public void formatsMultipleRelativeLiterals() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello $L, $L", "there", "guy");

        assertThat(result, equalTo("hello there, guy"));
    }

    @Test
    public void formatsMultipleRelativeLiteralsInBraces() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello ${L}, ${L}", "there", "guy");

        assertThat(result, equalTo("hello there, guy"));
    }

    @Test
    public void ensuresAllRelativeArgumentsWereUsed() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $L", "a", "b", "c");
        });
    }

    @Test
    public void performsRelativeBoundsChecking() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $L");
        });
    }

    @Test
    public void validatesThatDollarIsNotAtEof() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $");
        });
    }

    @Test
    public void validatesThatCustomStartIsNotAtEof() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.setExpressionStart('#');
            writer.format("hello #");
        });
    }

    @Test
    public void formatsPositionalLiterals() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello $1L", "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void formatsPositionalLiteralsWithCustomStart() {
        SimpleCodeWriter writer = createWriter();
        writer.setExpressionStart('#');
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello #1L", "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void formatsMultiplePositionalLiterals() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello $1L, $2L. $2L? You $1L?", "there", "guy");

        assertThat(result, equalTo("hello there, guy. guy? You there?"));
    }

    @Test
    public void formatsMultiplePositionalLiteralsInBraces() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello ${1L}, ${2L}. ${2L}? You ${1L}?", "there", "guy");

        assertThat(result, equalTo("hello there, guy. guy? You there?"));
    }

    @Test
    public void formatsMultipleDigitPositionalLiterals() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("$1L $2L $3L $4L $5L $6L $7L $8L $9L $10L $11L",
                "1",
                "2",
                "3",
                "4",
                "5",
                "6",
                "7",
                "8",
                "9",
                "10",
                "11");

        assertThat(result, equalTo("1 2 3 4 5 6 7 8 9 10 11"));
    }

    @Test
    public void performsPositionalBoundsChecking() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.write("Foo!");
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $1L");
        });

        assertThat(e.getMessage(),
                containsString("Positional argument index 0 out of range of provided 0 arguments "
                        + "in format string"));
    }

    @Test
    public void performsPositionalBoundsCheckingNotZero() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $0L", "a");
        });
    }

    @Test
    public void validatesThatPositionalIsNotAtEof() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $2");
        });
    }

    @Test
    public void validatesThatAllPositionalsAreUsed() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $2L $3L", "a", "b", "c", "d");
        });
    }

    @Test
    public void cannotMixPositionalAndRelative() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $1L, $L", "there");
        });
    }

    @Test
    public void cannotMixRelativeAndPositional() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $L, $1L", "there");
        });
    }

    @Test
    public void formatsNamedValues() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        writer.putContext("a", "a");
        writer.putContext("abc_def", "b");
        String result = writer.format("$a:L $abc_def:L");

        assertThat(result, equalTo("a b"));
    }

    @Test
    public void formatsNamedValuesInBraces() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        writer.putContext("a", "a");
        writer.putContext("abc_def", "b");
        String result = writer.format("${a:L} ${abc_def:L}");

        assertThat(result, equalTo("a b"));
    }

    @Test
    public void ensuresNamedValuesHasColon() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $abc foo");
        });
    }

    @Test
    public void ensuresNamedValuesHasFormatterAfterColon() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $abc:");
        });
    }

    @Test
    public void allowsSeveralSpecialCharactersInNamedArguments() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        writer.putContext("foo.baz#Bar$bam", "hello");
        writer.putContext("foo_baz", "hello");
        assertThat(writer.format("$foo.baz#Bar$bam:L"), equalTo("hello"));
        assertThat(writer.format("$foo_baz:L"), equalTo("hello"));
    }

    @Test
    public void ensuresNamedValuesMatchRegex() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("$nope!:L");
        });
    }

    @Test
    public void formattersMustNotBeLowercase() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('a', CodeFormatterTest::valueOf);
        });
    }

    @Test
    public void formattersMustNotBeNumbers() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('1', CodeFormatterTest::valueOf);
        });
    }

    @Test
    public void formattersMustNotBeDollar() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.putFormatter('$', CodeFormatterTest::valueOf);
        });
    }

    @Test
    public void ensuresFormatterIsValid() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.format("$E", "hi");
        });
    }

    @Test
    public void expandsInlineSectionsWithDefaults() {
        SimpleCodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);

        assertThat(writer.format("${L@hello}", "default"), equalTo("default"));
    }

    @Test
    public void expandsInlineSectionsWithInterceptors() {
        SimpleCodeWriter writer = createWriter();
        writer.onSection("hello", text -> writer.writeInline("intercepted: " + text));
        writer.write("Foo ${L@hello} baz", "default");

        assertThat(writer.toString(), equalTo("Foo intercepted: default baz\n"));
    }

    @Test
    public void canUseEmptyPlaceHolders() {
        SimpleCodeWriter writer = createWriter();
        writer.write("<abc${L@attributes}>", "");

        assertThat(writer.toString(), equalTo("<abc>\n"));
    }

    @Test
    public void canUsePositionalArgumentsWithSectionDefaults() {
        SimpleCodeWriter writer = createWriter();
        writer.write("<abc${1L@attributes}>", " a=\"Hi\"");

        assertThat(writer.toString(), equalTo("<abc a=\"Hi\">\n"));
    }

    @Test
    public void canUseOtherFormattersWithSections() {
        SimpleCodeWriter writer = createWriter();
        writer.onSection("foo", text -> writer.writeInline(text + "!"));
        writer.write("<abc foo=${S@attributes}>${S@foo}</abc>", "foo!", "baz");

        assertThat(writer.toString(), equalTo("<abc foo=\"foo!\">\"baz\"!</abc>\n"));
    }

    @Test
    public void cannotExpandInlineSectionOutsideOfBrace() {
        SimpleCodeWriter writer = createWriter();
        writer.write("Foo $L@hello baz", "default");
        assertThat(writer.toString(), equalTo("Foo default@hello baz\n"));
    }

    @Test
    public void inlineSectionNamesMustBeValid() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.write("${L@foo!}", "default");
        });
    }

    @Test
    public void inlineAlignmentMustOccurInBraces() {
        SimpleCodeWriter writer = createWriter();
        writer.write("  $L|", "a\nb");

        assertThat(writer.toString(), equalTo("  a\nb|\n"));
    }

    @Test
    public void detectsBlockAlignmentEof() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleCodeWriter writer = createWriter();
            writer.write("${L|", "default");
        });
    }

    @Test
    public void expandsAlignedRelativeFormatters() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("$L: ${L|}", "Names", "Bob\nKaren\nLuis");

        assertThat(writer.toString(), equalTo("Names: Bob\n       Karen\n       Luis\n"));
    }

    @Test
    public void expandsAlignedPositionalFormatters() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("$1L: ${2L|}", "Names", "Bob\nKaren\nLuis");

        assertThat(writer.toString(), equalTo("Names: Bob\n       Karen\n       Luis\n"));
    }

    @Test
    public void expandsAlignedRelativeFormattersWithWindowsNewlines() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("$L: ${L|}", "Names", "Bob\r\nKaren\r\nLuis");

        assertThat(writer.toString(), equalTo("Names: Bob\r\n       Karen\r\n       Luis\n"));
    }

    @Test
    public void expandsAlignedRelativeFormattersWithCarriageReturns() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("$L: ${L|}", "Names", "Bob\rKaren\rLuis");

        assertThat(writer.toString(), equalTo("Names: Bob\r       Karen\r       Luis\n"));
    }

    @Test
    public void expandsAlignedBlocksWithNewlines() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("$1L() {\n" +
                "    ${2L|}\n" +
                "}", "method", "// this\n// is a test.");

        assertThat(writer.toString(), equalTo("method() {\n    // this\n    // is a test.\n}\n"));
    }

    @Test
    public void alignedBlocksComposeWithPrefixes() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.setNewlinePrefix("| ");
        writer.write("$1L() {\n" +
                "    ${2L|}\n" +
                "}", "method", "// this\n// is a test.");

        assertThat(writer.toString(), equalTo("| method() {\n|     // this\n|     // is a test.\n| }\n"));
    }

    @Test
    public void defaultCFormatterRequiresRunnableOrFunction() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> new CodeWriter().write("$C", "hi"));

        assertThat(e.getMessage(),
                containsString(
                        "Expected value for 'C' formatter to be an instance of " + Runnable.class.getName()
                                + " or " + Consumer.class.getName() + ", but found " + String.class.getName()));
    }

    @Test
    public void cFormaterAcceptsConsumersThatAreCodeWriters() {
        SimpleCodeWriter w = new SimpleCodeWriter();
        w.write("$C", (Consumer<SimpleCodeWriter>) writer -> writer.write("Hello!"));

        assertThat(w.toString(), equalTo("Hello!\n"));
    }

    @Test
    public void cFormatterAcceptsConsumersThatAreSubtypesOfCodeWriters() {
        CodeWriterSubtype w = new CodeWriterSubtype();
        w.write("$C", w.consumer(writer -> writer.write2("Hello!")));

        assertThat(w.toString(), equalTo("Hello!\n"));
    }

    // This class makes sure that subtypes of CodeWriter can be called from the C
    // formatter using an unsafe cast.
    static final class CodeWriterSubtype extends AbstractCodeWriter<CodeWriterSubtype> {
        void write2(String text) {
            write(text);
        }
    }

    @Test
    public void alignsBlocksWithStaticWhitespace() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("$1L() {\n" +
                "\t\t${2L|}\n" +
                "}", "method", "hi\nthere");

        assertThat(writer.toString(), equalTo("method() {\n\t\thi\n\t\tthere\n}\n"));
    }

    @Test
    public void alignsBlocksWithStaticAndSpecificWhitespace() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("$1L() {\n" +
                "\t\t  ${2L|}\n" +
                "}", "method", "hi\nthere");

        assertThat(writer.toString(), equalTo("method() {\n\t\t  hi\n\t\t  there\n}\n"));
    }

    @Test
    public void canAlignNestedBlocks() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.write("$L() {\n\t\t${C|}\n}", "a", (Runnable) () -> {
            writer.write("$L() {\n\t\t${C|}\n}", "b", (Runnable) () -> {
                writer.write("$L() {\n\t\t  ${C|}\n}", "c", (Runnable) () -> {
                    writer.write("d");
                });
            });
        });

        assertThat(writer.toString(),
                equalTo("a() {\n"
                        + "\t\tb() {\n"
                        + "\t\t\t\tc() {\n"
                        + "\t\t\t\t\t\t  d\n"
                        + "\t\t\t\t}\n"
                        + "\t\t}\n"
                        + "}\n"));
    }

    @Test
    public void canProvideCustomLoopPrefixes() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.putContext("foo", Arrays.asList("a", "b", "c"));
        writer.write("${#foo as k, v}\n"
                + "${k:L} ${v:L} ${k.first:L} ${k.last:L}\n"
                + "${/foo}");

        assertThat(writer.toString(),
                equalTo("0 a true false\n"
                        + "1 b false false\n"
                        + "2 c false true\n"));
    }

    @Test
    public void validatesKeyBindingName() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        RuntimeException e = Assertions.assertThrows(RuntimeException.class,
                () -> writer.write("${#foo as 0a, v}${/foo}"));

        assertThat(e.getMessage(), containsString("Invalid format expression name `0a`"));
    }

    @Test
    public void validatesValueBindingName() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        RuntimeException e = Assertions.assertThrows(RuntimeException.class,
                () -> writer.write("${#foo as k, 0v}${/foo}"));

        assertThat(e.getMessage(), containsString("Invalid format expression name `0v`"));
    }

    @Test
    public void requiresBothKeyAndValueBindingNamesWhenAnySet() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        RuntimeException e = Assertions.assertThrows(RuntimeException.class,
                () -> writer.write("${#foo as k}${/foo}"));

        assertThat(e.getMessage(), containsString("Expected: ',', but found '}'"));
    }

    @Test
    public void canNestForLoops() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.putContext("foo", Arrays.asList("a", "b", "c"));
        writer.write("${#foo as k1, v1}\n"
                + "${#foo as k2, v2}\n"
                + "${k1:L} ${v1:L}; ${k2:L} ${v2:L}\n"
                + "${/foo}\n"
                + "${/foo}");

        assertThat(writer.toString(),
                equalTo("0 a; 0 a\n"
                        + "0 a; 1 b\n"
                        + "0 a; 2 c\n"
                        + "1 b; 0 a\n"
                        + "1 b; 1 b\n"
                        + "1 b; 2 c\n"
                        + "2 c; 0 a\n"
                        + "2 c; 1 b\n"
                        + "2 c; 2 c\n"));
    }

    @Test
    public void controlsLeadingWhitespace() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.putContext("foo", Arrays.asList("a", "b", "c"));
        writer.write("Hey.   \n  ${~L}", "hi");

        assertThat(writer.toString(), equalTo("Hey.hi"));
    }

    @Test
    public void controlsLeadingAndTrailingWhitespace() {
        SimpleCodeWriter writer = new SimpleCodeWriter().trimTrailingSpaces(false).insertTrailingNewline(false);
        writer.putContext("foo", Arrays.asList("a", "b", "c"));
        writer.write("Hey.   \n  ${~L~}    \n.Bye.", "hi");

        assertThat(writer.toString(), equalTo("Hey.hi.Bye."));
    }

    @Test
    public void leadingWhitespaceNoOp() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.putContext("foo", "hi");
        writer.write("${~foo:L}");

        assertThat(writer.toString(), equalTo("hi\n"));
    }

    @Test
    public void controlsLeadingWhitespaceWithConditionals() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.putContext("foo", Arrays.asList("a", "b", "c"));
        writer.write("${#foo as k1, v1}\n" // elided because it's standalone
                + "  ${#foo as k2, v2}\n" // elided because it's standalone
                + "      ${~k1:L} ${v1:L}; ${k2:L} ${v2:L}\n" // removes leading ws
                + "  ${/foo}\n" // elided
                + "${/foo}"); // elided

        assertThat(writer.toString(),
                equalTo("0 a; 0 a\n"
                        + "0 a; 1 b\n"
                        + "0 a; 2 c\n"
                        + "1 b; 0 a\n"
                        + "1 b; 1 b\n"
                        + "1 b; 2 c\n"
                        + "2 c; 0 a\n"
                        + "2 c; 1 b\n"
                        + "2 c; 2 c\n"));
    }

    @Test
    public void controlsLeadingWhitespaceWithConditionalsAndBlocks() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.putContext("foo", "text");
        writer.write("${?foo}\n" // elided because it's standalone
                + "  ${~foo:L} ${C|}\n" // removes leading ws before v, and C is properly formatted.
                + "${/foo}", // elided
                writer.consumer(w -> w.writeInlineWithNoFormatting("hi1\nhi2")));

        assertThat(writer.toString(),
                equalTo("text hi1\n"
                        + "     hi2\n"));
    }

    @Test
    public void cannotUseInlineBlockAlignmentWithTrimmedWhitespace() {
        SimpleCodeWriter writer = new SimpleCodeWriter();

        Assertions.assertThrows(RuntimeException.class,
                () -> writer.write("${~C|}", writer.consumer(w -> w.write("x"))));
    }

    @Test
    public void canStripTrailingWsInFormattedExpansion() {
        SimpleCodeWriter writer = new SimpleCodeWriter();
        writer.putContext("foo", "hi");
        writer.write("${foo:L~}\n  \n  .");

        assertThat(writer.toString(), equalTo("hi.\n"));
    }

    @Test
    public void canSkipTrailingWhitespaceInConditionals() {
        SimpleCodeWriter writer = new SimpleCodeWriter().insertTrailingNewline(false);
        writer.putContext("nav", "http://example.com");
        writer.write("${?nav~}\n"
                + "  <a href=\"${nav:L}\">${nav:L}</a>\n"
                + "${~/nav}");

        assertThat(writer.toString(), equalTo("<a href=\"http://example.com\">http://example.com</a>"));
    }
}

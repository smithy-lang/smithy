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

public class CodeFormatterTest {

    private CodeWriter createWriter() {
        return CodeWriter.createDefault();
    }

    private static String valueOf(Object value, String indent) {
        return String.valueOf(value);
    }

    @Test
    public void formatsDollarLiterals() {
        CodeFormatter formatter = new CodeFormatter();
        String result = formatter.format('$', "hello $$", "", createWriter());

        assertThat(result, equalTo("hello $"));
    }

    @Test
    public void formatsRelativeLiterals() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        String result = formatter.format('$', "hello $L", "", createWriter(), "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void formatsRelativeLiteralsInBraces() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        String result = formatter.format('$', "hello ${L}", "", createWriter(), "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void requiresTextAfterOpeningBrace() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.format('$', "hello ${", "", createWriter(), "there");
        });
    }

    @Test
    public void requiresBraceIsClosed() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello ${L .", "", createWriter(), "there");
        });
    }

    @Test
    public void formatsMultipleRelativeLiterals() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        String result = formatter.format('$', "hello $L, $L", "", createWriter(), "there", "guy");

        assertThat(result, equalTo("hello there, guy"));
    }

    @Test
    public void formatsMultipleRelativeLiteralsInBraces() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        String result = formatter.format('$', "hello ${L}, ${L}", "", createWriter(), "there", "guy");

        assertThat(result, equalTo("hello there, guy"));
    }

    @Test
    public void ensuresAllRelativeArgumentsWereUsed() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $L", "", createWriter(), "a", "b", "c");
        });
    }

    @Test
    public void performsRelativeBoundsChecking() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $L", "", createWriter());
        });
    }

    @Test
    public void validatesThatDollarIsNotAtEof() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $", "", createWriter());
        });
    }

    @Test
    public void validatesThatCustomStartIsNotAtEof() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('#', "hello #", "", createWriter());
        });
    }

    @Test
    public void formatsPositionalLiterals() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        String result = formatter.format('$', "hello $1L", "", createWriter(), "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void formatsPositionalLiteralsWithCustomStart() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        String result = formatter.format('#', "hello #1L", "", createWriter(), "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void formatsMultiplePositionalLiterals() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        String result = formatter.format('$', "hello $1L, $2L. $2L? You $1L?", "", createWriter(), "there", "guy");

        assertThat(result, equalTo("hello there, guy. guy? You there?"));
    }

    @Test
    public void formatsMultiplePositionalLiteralsInBraces() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        String result = formatter.format('$', "hello ${1L}, ${2L}. ${2L}? You ${1L}?", "", createWriter(), "there", "guy");

        assertThat(result, equalTo("hello there, guy. guy? You there?"));
    }

    @Test
    public void formatsMultipleDigitPositionalLiterals() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        String result = formatter.format('$', "$1L $2L $3L $4L $5L $6L $7L $8L $9L $10L $11L", "", createWriter(),
                                         "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

        assertThat(result, equalTo("1 2 3 4 5 6 7 8 9 10 11"));
    }

    @Test
    public void performsPositionalBoundsChecking() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $1L", "", createWriter());
        });
    }

    @Test
    public void performsPositionalBoundsCheckingNotZero() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $0L", "", createWriter(), "a");
        });
    }

    @Test
    public void validatesThatPositionalIsNotAtEof() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $2", "", createWriter());
        });
    }

    @Test
    public void validatesThatAllPositionalsAreUsed() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $2L $3L", "", createWriter(), "a", "b", "c", "d");
        });
    }

    @Test
    public void cannotMixPositionalAndRelative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $1L, $L", "", createWriter(), "there");
        });
    }

    @Test
    public void cannotMixRelativeAndPositional() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $L, $1L", "", createWriter(), "there");
        });
    }

    @Test
    public void formatsNamedValues() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        CodeWriter writer = createWriter();
        writer.putContext("a", "a");
        writer.putContext("abc_def", "b");
        String result = formatter.format('$', "$a:L $abc_def:L", "", writer);

        assertThat(result, equalTo("a b"));
    }

    @Test
    public void formatsNamedValuesInBraces() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        CodeWriter writer = createWriter();
        writer.putContext("a", "a");
        writer.putContext("abc_def", "b");
        String result = formatter.format('$', "${a:L} ${abc_def:L}", "", writer);

        assertThat(result, equalTo("a b"));
    }

    @Test
    public void ensuresNamedValuesHasColon() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $abc foo", "", createWriter());
        });
    }

    @Test
    public void ensuresNamedValuesHasFormatterAfterColon() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "hello $abc:", "", createWriter());
        });
    }

    @Test
    public void allowsSeveralSpecialCharactersInNamedArguments() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);

        CodeWriter writer = createWriter();
        writer.putContext("foo.baz#Bar$bam", "hello");
        writer.putContext("foo_baz", "hello");
        assertThat(formatter.format('$', "$foo.baz#Bar$bam:L", "", writer), equalTo("hello"));
        assertThat(formatter.format('$', "$foo_baz:L", "", writer), equalTo("hello"));
    }

    @Test
    public void ensuresNamedValuesMatchRegex() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('L', CodeFormatterTest::valueOf);
            formatter.format('$', "$nope!:L", "", createWriter());
        });
    }

    @Test
    public void formattersMustNotBeLowercase() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('a', CodeFormatterTest::valueOf);
        });
    }

    @Test
    public void formattersMustNotBeNumbers() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('1', CodeFormatterTest::valueOf);
        });
    }

    @Test
    public void formattersMustNotBeDollar() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.putFormatter('$', CodeFormatterTest::valueOf);
        });
    }

    @Test
    public void ensuresFormatterIsValid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeFormatter formatter = new CodeFormatter();
            formatter.format('$', "$L", "", createWriter(), "hi");
        });
    }

    @Test
    public void expandsInlineSectionsWithDefaults() {
        CodeFormatter formatter = new CodeFormatter();
        formatter.putFormatter('L', CodeFormatterTest::valueOf);
        CodeWriter writer = createWriter();

        assertThat(formatter.format('$', "${L@hello}", "", writer, "default"), equalTo("default"));
    }

    @Test
    public void expandsInlineSectionsWithInterceptors() {
        CodeWriter writer = createWriter();
        writer.onSection("hello", text -> writer.write("intercepted: " + text));
        writer.write("Foo ${L@hello} baz", "default");

        assertThat(writer.toString(), equalTo("Foo intercepted: default baz\n"));
    }

    @Test
    public void canUseEmptyPlaceHolders() {
        CodeWriter writer = createWriter();
        writer.write("<abc${L@attributes}>", "");

        assertThat(writer.toString(), equalTo("<abc>\n"));
    }

    @Test
    public void canUsePositionalArgumentsWithSectionDefaults() {
        CodeWriter writer = createWriter();
        writer.write("<abc${1L@attributes}>", " a=\"Hi\"");

        assertThat(writer.toString(), equalTo("<abc a=\"Hi\">\n"));
    }

    @Test
    public void canUseOtherFormattersWithSections() {
        CodeWriter writer = createWriter();
        writer.onSection("foo", text -> writer.write(text + "!"));
        writer.write("<abc foo=${S@attributes}>${S@foo}</abc>", "foo!", "baz");

        assertThat(writer.toString(), equalTo("<abc foo=\"foo!\">\"baz\"!</abc>\n"));
    }

    @Test
    public void cannotExpandInlineSectionOutsideOfBrace() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeWriter writer = createWriter();
            writer.write("Foo $L@hello baz", "default");
        });
    }

    @Test
    public void inlineSectionNamesMustBeValid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeWriter writer = createWriter();
            writer.write("${L@foo!}", "default");
        });
    }

    @Test
    public void inlineAlignmentMustOccurInBraces() {
        CodeWriter writer = createWriter();
        writer.write("  $L|", "a\nb");

        assertThat(writer.toString(), equalTo("  a\nb|\n"));
    }

    @Test
    public void detectsBlockAlignmentEof() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeWriter writer = createWriter();
            writer.write("${L|", "default");
        });
    }

    @Test
    public void expandsAlignedRelativeFormatters() {
        CodeWriter writer = new CodeWriter();
        writer.write("$L: ${L|}", "Names", "Bob\nKaren\nLuis");

        assertThat(writer.toString(), equalTo("Names: Bob\n       Karen\n       Luis\n"));
    }

    @Test
    public void expandsAlignedPositionalFormatters() {
        CodeWriter writer = new CodeWriter();
        writer.write("$1L: ${2L|}", "Names", "Bob\nKaren\nLuis");

        assertThat(writer.toString(), equalTo("Names: Bob\n       Karen\n       Luis\n"));
    }

    @Test
    public void expandsAlignedRelativeFormattersWithWindowsNewlines() {
        CodeWriter writer = new CodeWriter();
        writer.write("$L: ${L|}", "Names", "Bob\r\nKaren\r\nLuis");

        assertThat(writer.toString(), equalTo("Names: Bob\r\n       Karen\r\n       Luis\n"));
    }

    @Test
    public void expandsAlignedRelativeFormattersWithCarriageReturns() {
        CodeWriter writer = new CodeWriter();
        writer.write("$L: ${L|}", "Names", "Bob\rKaren\rLuis");

        assertThat(writer.toString(), equalTo("Names: Bob\r       Karen\r       Luis\n"));
    }

    @Test
    public void expandsAlignedBlocksWithNewlines() {
        CodeWriter writer = new CodeWriter();
        writer.write("$1L() {\n" +
                     "    ${2L|}\n" +
                     "}", "method", "// this\n// is a test.");

        assertThat(writer.toString(), equalTo("method() {\n    // this\n    // is a test.\n}\n"));
    }

    @Test
    public void alignedBlocksComposeWithPrefixes() {
        CodeWriter writer = new CodeWriter();
        writer.setNewlinePrefix("| ");
        writer.write("$1L() {\n" +
                     "    ${2L|}\n" +
                     "}", "method", "// this\n// is a test.");

        assertThat(writer.toString(), equalTo("| method() {\n|     // this\n|     // is a test.\n| }\n"));
    }
}

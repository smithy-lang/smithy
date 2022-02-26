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
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Consumer;
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
        CodeWriter writer = createWriter();
        String result = writer.format("hello $$.");

        assertThat(result, equalTo("hello $."));
    }

    @Test
    public void formatsRelativeLiterals() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello $L", "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void formatsRelativeLiteralsInBraces() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello ${L}", "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void requiresTextAfterOpeningBrace() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.format("hello ${", "there");
        });

        assertThat(e.getMessage(), containsString("expected one of the following tokens: '!'"));
    }

    @Test
    public void requiresBraceIsClosed() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello ${L .", "there");
        });
    }

    @Test
    public void formatsMultipleRelativeLiterals() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello $L, $L", "there", "guy");

        assertThat(result, equalTo("hello there, guy"));
    }

    @Test
    public void formatsMultipleRelativeLiteralsInBraces() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello ${L}, ${L}", "there", "guy");

        assertThat(result, equalTo("hello there, guy"));
    }

    @Test
    public void ensuresAllRelativeArgumentsWereUsed() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $L", "a", "b", "c");
        });
    }

    @Test
    public void performsRelativeBoundsChecking() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $L");
        });
    }

    @Test
    public void validatesThatDollarIsNotAtEof() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $");
        });
    }

    @Test
    public void validatesThatCustomStartIsNotAtEof() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.setExpressionStart('#');
            writer.format("hello #");
        });
    }

    @Test
    public void formatsPositionalLiterals() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello $1L", "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void formatsPositionalLiteralsWithCustomStart() {
        CodeWriter writer = createWriter();
        writer.setExpressionStart('#');
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello #1L", "there");

        assertThat(result, equalTo("hello there"));
    }

    @Test
    public void formatsMultiplePositionalLiterals() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello $1L, $2L. $2L? You $1L?", "there", "guy");

        assertThat(result, equalTo("hello there, guy. guy? You there?"));
    }

    @Test
    public void formatsMultiplePositionalLiteralsInBraces() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("hello ${1L}, ${2L}. ${2L}? You ${1L}?", "there", "guy");

        assertThat(result, equalTo("hello there, guy. guy? You there?"));
    }

    @Test
    public void formatsMultipleDigitPositionalLiterals() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        String result = writer.format("$1L $2L $3L $4L $5L $6L $7L $8L $9L $10L $11L",
                                      "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

        assertThat(result, equalTo("1 2 3 4 5 6 7 8 9 10 11"));
    }

    @Test
    public void performsPositionalBoundsChecking() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.write("Foo!");
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $1L");
        });

        assertThat(e.getMessage(), containsString("Positional argument index 0 out of range of provided 0 arguments "
                                                  + "in format string"));
    }

    @Test
    public void performsPositionalBoundsCheckingNotZero() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $0L", "a");
        });
    }

    @Test
    public void validatesThatPositionalIsNotAtEof() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $2");
        });
    }

    @Test
    public void validatesThatAllPositionalsAreUsed() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $2L $3L", "a", "b", "c", "d");
        });
    }

    @Test
    public void cannotMixPositionalAndRelative() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $1L, $L", "there");
        });
    }

    @Test
    public void cannotMixRelativeAndPositional() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $L, $1L", "there");
        });
    }

    @Test
    public void formatsNamedValues() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        writer.putContext("a", "a");
        writer.putContext("abc_def", "b");
        String result = writer.format("$a:L $abc_def:L");

        assertThat(result, equalTo("a b"));
    }

    @Test
    public void formatsNamedValuesInBraces() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        writer.putContext("a", "a");
        writer.putContext("abc_def", "b");
        String result = writer.format("${a:L} ${abc_def:L}");

        assertThat(result, equalTo("a b"));
    }

    @Test
    public void ensuresNamedValuesHasColon() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $abc foo");
        });
    }

    @Test
    public void ensuresNamedValuesHasFormatterAfterColon() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("hello $abc:");
        });
    }

    @Test
    public void allowsSeveralSpecialCharactersInNamedArguments() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);
        writer.putContext("foo.baz#Bar$bam", "hello");
        writer.putContext("foo_baz", "hello");
        assertThat(writer.format("$foo.baz#Bar$bam:L"), equalTo("hello"));
        assertThat(writer.format("$foo_baz:L"), equalTo("hello"));
    }

    @Test
    public void ensuresNamedValuesMatchRegex() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('L', CodeFormatterTest::valueOf);
            writer.format("$nope!:L");
        });
    }

    @Test
    public void formattersMustNotBeLowercase() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('a', CodeFormatterTest::valueOf);
        });
    }

    @Test
    public void formattersMustNotBeNumbers() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('1', CodeFormatterTest::valueOf);
        });
    }

    @Test
    public void formattersMustNotBeDollar() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.putFormatter('$', CodeFormatterTest::valueOf);
        });
    }

    @Test
    public void ensuresFormatterIsValid() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            CodeWriter writer = createWriter();
            writer.format("$E", "hi");
        });
    }

    @Test
    public void expandsInlineSectionsWithDefaults() {
        CodeWriter writer = createWriter();
        writer.putFormatter('L', CodeFormatterTest::valueOf);

        assertThat(writer.format("${L@hello}", "default"), equalTo("default"));
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
        CodeWriter writer = createWriter();
        writer.write("Foo $L@hello baz", "default");
        assertThat(writer.toString(), equalTo("Foo default@hello baz\n"));
    }

    @Test
    public void inlineSectionNamesMustBeValid() {
        Assertions.assertThrows(RuntimeException.class, () -> {
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
        Assertions.assertThrows(RuntimeException.class, () -> {
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

    @Test
    public void defaultCFormatterRequiresRunnableOrFunction() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> new CodeWriter().write("$C", "hi"));

        assertThat(e.getMessage(), containsString(
                "Expected value for 'C' formatter to be an instance of " + Runnable.class.getName()
                + " or " + Consumer.class.getName() + ", but found " + String.class.getName()));
    }

    @Test
    public void cFormaterAcceptsConsumersThatAreCodeWriters() {
        CodeWriter w = new CodeWriter();
        w.write("$C", (Consumer<CodeWriter>) writer -> writer.write("Hello!"));

        assertThat(w.toString(), equalTo("Hello!\n"));
    }

    @Test
    public void cFormatterAcceptsConsumersThatAreSubtypesOfCodeWriters() {
        CodeWriterSubtype w = new CodeWriterSubtype();
        w.write("$C", w.call(writer -> writer.write2("Hello!")));

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
        CodeWriter writer = new CodeWriter();
        writer.write("$1L() {\n" +
                     "\t\t${2L|}\n" +
                     "}", "method", "hi\nthere");

        assertThat(writer.toString(), equalTo("method() {\n\t\thi\n\t\tthere\n}\n"));
    }

    @Test
    public void alignsBlocksWithStaticAndSpecificWhitespace() {
        CodeWriter writer = new CodeWriter();
        writer.write("$1L() {\n" +
                     "\t\t  ${2L|}\n" +
                     "}", "method", "hi\nthere");

        assertThat(writer.toString(), equalTo("method() {\n\t\t  hi\n\t\t  there\n}\n"));
    }

    @Test
    public void canAlignNestedBlocks() {
        CodeWriter writer = new CodeWriter();
        writer.write("$L() {\n\t\t${C|}\n}", "a", (Runnable) () -> {
            writer.write("$L() {\n\t\t${C|}\n}", "b", (Runnable) () -> {
                writer.write("$L() {\n\t\t  ${C|}\n}", "c", (Runnable) () -> {
                    writer.write("d");
                });
            });
        });

        assertThat(writer.toString(), equalTo("a() {\n"
                                              + "\t\tb() {\n"
                                              + "\t\t\t\tc() {\n"
                                              + "\t\t\t\t\t\t  d\n"
                                              + "\t\t\t\t}\n"
                                              + "\t\t}\n"
                                              + "}\n"));
    }
}

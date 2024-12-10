/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringUtilsTest {
    @Test
    public void uppercaseFirst() {
        assertThat(StringUtils.capitalize("foo"), equalTo("Foo"));
        assertThat(StringUtils.capitalize(" foo"), equalTo(" foo"));
        assertThat(StringUtils.capitalize("10-foo"), equalTo("10-foo"));
        assertThat(StringUtils.capitalize("_foo"), equalTo("_foo"));
    }

    @Test
    public void lowercaseFirst() {
        assertThat(StringUtils.uncapitalize("foo"), equalTo("foo"));
        assertThat(StringUtils.uncapitalize(" foo"), equalTo(" foo"));
        assertThat(StringUtils.uncapitalize("10-foo"), equalTo("10-foo"));
        assertThat(StringUtils.uncapitalize("_foo"), equalTo("_foo"));
        assertThat(StringUtils.uncapitalize("Foo"), equalTo("foo"));
        assertThat(StringUtils.uncapitalize(" Foo"), equalTo(" Foo"));
        assertThat(StringUtils.uncapitalize("10-Foo"), equalTo("10-Foo"));
        assertThat(StringUtils.uncapitalize("_Foo"), equalTo("_Foo"));
    }

    @Test
    public void capitalizesAndUncapitalizes() {
        assertThat(StringUtils.capitalize(null), equalTo(null));
        assertThat(StringUtils.uncapitalize(null), equalTo(null));

        assertThat(StringUtils.capitalize(""), equalTo(""));
        assertThat(StringUtils.uncapitalize(""), equalTo(""));

        assertThat(StringUtils.capitalize("Foo"), equalTo("Foo"));
        assertThat(StringUtils.uncapitalize("Foo"), equalTo("foo"));

        assertThat(StringUtils.capitalize("foo"), equalTo("Foo"));
        assertThat(StringUtils.uncapitalize("foo"), equalTo("foo"));
    }

    @Test
    public void wrapsText() {
        assertThat(StringUtils.wrap("hello, there, bud", 6), equalTo(String.format("hello,%nthere,%nbud")));
    }

    @Test
    public void indentsText() {
        assertEquals(StringUtils.indent("foo", 2), "  foo");
        assertEquals(StringUtils.indent(" foo", 2), "   foo");
        assertEquals(StringUtils.indent(String.format("foo%nbar"), 2), String.format("  foo%n  bar"));
        assertEquals(StringUtils.indent(String.format("%nbar"), 2), String.format("  %n  bar"));
        assertEquals(StringUtils.indent(String.format("foo%n"), 2), String.format("  foo%n"));
    }

    // These test cases are based on https://github.com/square/javapoet/blob/master/src/test/java/com/squareup/javapoet/UtilTest.java
    @Test
    public void stringLiteral() {
        stringLiteral("abc", "abc", "");
        stringLiteral("'", "'", "");
        stringLiteral("♦♥♠♣", "♦♥♠♣", "");
        stringLiteral("€\\t@\\t$", "€\t@\t$", "");
        stringLiteral("abc();\\ndef();", "abc();\ndef();", "");
        stringLiteral("This is \\\"quoted\\\"!", "This is \"quoted\"!", "");
        stringLiteral("e^{i\\\\pi}+1=0", "e^{i\\pi}+1=0", "");
    }

    void stringLiteral(String expected, String value, String indent) {
        assertThat("\"" + expected + "\"", equalTo(StringUtils.escapeJavaString(value, indent)));
    }
}

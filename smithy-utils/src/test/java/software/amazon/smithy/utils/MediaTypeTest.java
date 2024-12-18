/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MediaTypeTest {
    @Test
    public void requiresNonEmptyString() {
        Assertions.assertThrows(RuntimeException.class, () -> MediaType.from(""));
    }

    @Test
    public void requiresValidTypeToken() {
        Assertions.assertThrows(RuntimeException.class, () -> MediaType.from(","));
    }

    @Test
    public void requiresSubtype() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> MediaType.from("application"));

        assertThat(e.getMessage(), containsString("Expected: '/'"));
    }

    @Test
    public void requiresSubtypeToken() {
        Assertions.assertThrows(RuntimeException.class, () -> MediaType.from("application/"));
    }

    @Test
    public void requiresParametersAfterSemicolon() {
        Assertions.assertThrows(RuntimeException.class, () -> MediaType.from("application/json;"));
    }

    @Test
    public void requiresValidParameterName() {
        Assertions.assertThrows(RuntimeException.class, () -> MediaType.from("application/json; \\"));
    }

    @Test
    public void requiresEqualsAfterParameterName() {
        RuntimeException e = Assertions.assertThrows(
                RuntimeException.class,
                () -> MediaType.from("application/json; foo"));

        assertThat(e.getMessage(), containsString("Expected: '='"));
    }

    @Test
    public void requiresParameterValueAfterEquals() {
        Assertions.assertThrows(RuntimeException.class, () -> MediaType.from("application/json; foo="));
    }

    @Test
    public void requiresParameterValueClosingQuote() {
        Assertions.assertThrows(RuntimeException.class, () -> MediaType.from("application/json; foo=\"baz"));
    }

    @Test
    public void requiresParameterValueAfterOtherParamsAndSemicolon() {
        Assertions.assertThrows(RuntimeException.class, () -> MediaType.from("application/json; foo=\"baz\"; "));
    }

    @Test
    public void detectsInvalidQuotedEscapes() {
        RuntimeException e = Assertions.assertThrows(
                RuntimeException.class,
                () -> MediaType.from("application/json; foo=\"bar\\"));

        assertThat(e.getMessage(), containsString("Expected character after escape"));
    }

    @Test
    public void detectsJsonMediaTypes() {
        assertThat(MediaType.isJson("application/json"), is(true));
        assertThat(MediaType.isJson("application/foo+json"), is(true));
        assertThat(MediaType.isJson("foo/json"), is(false));
        assertThat(MediaType.isJson("application/jsonn"), is(false));
        assertThat(MediaType.isJson("application/foo+jsonn"), is(false));
    }

    @Test
    public void parsesSuffix() {
        assertThat(MediaType.from("foo/baz+").getSubtypeWithoutSuffix(), equalTo("baz"));
        assertThat(MediaType.from("foo/baz+").getSuffix(), equalTo(Optional.empty()));
        assertThat(MediaType.from("foo/baz+bam+boo").getSuffix(), equalTo(Optional.of("boo")));
        assertThat(MediaType.from("foo/baz+bam+boo").getSubtypeWithoutSuffix(), equalTo("baz+bam"));
    }

    @Test
    public void parsesMediaType() {
        String typeString = "foo/baz; bar=abc;BAM=\"100\"  ;  _a=_";
        MediaType type = MediaType.from(typeString);

        assertThat(typeString, equalTo(type.toString()));

        assertThat(type.getType(), equalTo("foo"));
        assertThat(type.getSubtype(), equalTo("baz"));

        assertThat(type.getParameters(), hasKey("bar"));
        assertThat(type.getParameters(), hasKey("bam"));
        assertThat(type.getParameters(), hasKey("_a"));

        assertThat(type.getParameters().get("bar"), equalTo("abc"));
        assertThat(type.getParameters().get("bam"), equalTo("100"));
        assertThat(type.getParameters().get("_a"), equalTo("_"));
    }

    @Test
    public void hashCodeAndEquals() {
        String typeString = "foo/baz; bar=abc;BAM=\"100\"  ;  _a=_";
        MediaType type = MediaType.from(typeString);
        MediaType type2 = MediaType.from(typeString);
        MediaType type3 = MediaType.from("foo/baz; bar=abc");

        assertThat(type, equalTo(type));
        assertThat(type, equalTo(type2));
        assertThat(type.hashCode(), equalTo(type2.hashCode()));

        assertThat(type, not(equalTo(type3)));
        assertThat(type.hashCode(), not(equalTo(type3.hashCode())));
        assertThat(type, not(equalTo("foo")));
    }

    @Test
    public void allowsEscapedQuotes() {
        MediaType type = MediaType.from("foo/baz; bar=\"foo\\\"baz\"");

        assertThat(type.getParameters().get("bar"), equalTo("foo\"baz"));
    }

    @Test
    public void allowsSpecialStuffInQuotes() {
        MediaType type = MediaType.from("foo/baz; bar=\";\"");

        assertThat(type.getParameters().get("bar"), equalTo(";"));
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.functions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Substring;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.UriEncode;

public class FunctionsTest {
    @Test
    public void booleanEqualsOfExpressions() {
        BooleanEquals function = BooleanEquals.ofExpressions(Expression.of(true), true);
        assertThat(function, instanceOf(BooleanEquals.class));

        BooleanEquals function2 = BooleanEquals.ofExpressions(Expression.of(true), Expression.of(true));
        assertThat(function2, instanceOf(BooleanEquals.class));
    }

    @Test
    public void getAttrOfExpressions() {
        GetAttr function = GetAttr.ofExpressions(
                Expression.getReference(Identifier.of("a"), SourceLocation.none()),
                "b");
        assertThat(function, instanceOf(GetAttr.class));

        GetAttr function2 = GetAttr.ofExpressions(
                Expression.getReference(Identifier.of("a"), SourceLocation.none()),
                Expression.of("b"));
        assertThat(function2, instanceOf(GetAttr.class));
    }

    @Test
    public void isValidHostLabelOfExpression() {
        IsValidHostLabel function = IsValidHostLabel.ofExpressions(Expression.of("foobar"), true);
        assertThat(function, instanceOf(IsValidHostLabel.class));

        IsValidHostLabel function2 = IsValidHostLabel.ofExpressions(Expression.of("foobar"), Expression.of(true));
        assertThat(function2, instanceOf(IsValidHostLabel.class));
    }

    @Test
    public void notOfExpression() {
        Not function = Not.ofExpressions(Expression.of(false));
        assertThat(function, instanceOf(Not.class));

        assertThat(Expression.of(true).not(), instanceOf(Not.class));
    }

    @Test
    public void parseUrlOfExpression() {
        ParseUrl function = ParseUrl.ofExpressions(Expression.of("http://example.com"));
        assertThat(function, instanceOf(ParseUrl.class));
    }

    @Test
    public void stringEqualsOfExpression() {
        StringEquals function = StringEquals.ofExpressions(Expression.of("foo"), "foo");
        assertThat(function, instanceOf(StringEquals.class));

        StringEquals function2 = StringEquals.ofExpressions(Expression.of("foo"), Expression.of("foo"));
        assertThat(function2, instanceOf(StringEquals.class));
    }

    @Test
    public void substringOfExpression() {
        Substring function = Substring.ofExpressions(Expression.of("foobar"), 1, 2, false);
        assertThat(function, instanceOf(Substring.class));

        Substring function2 = Substring.ofExpressions(
                Expression.of("foobar"),
                Expression.of(1),
                Expression.of(2),
                Expression.of(false));
        assertThat(function2, instanceOf(Substring.class));
    }

    @Test
    public void uriEncodeOfExpression() {
        UriEncode function = UriEncode.ofExpressions(Expression.of("foo bar%baz"));
        assertThat(function, instanceOf(UriEncode.class));
    }
}

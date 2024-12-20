/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.functions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Substring;

public class SyntaxElementTest {
    @Test
    void booleanEquals() {
        Expression bool = Expression.of(true);
        BooleanEquals booleanEquals = bool.booleanEqual(false);
        assertTrue(booleanEquals.getArguments().contains(bool));
        assertTrue(booleanEquals.getArguments().contains(Expression.of(false)));
    }

    @Test
    void stringEquals() {
        Expression str = Expression.of("foo");
        StringEquals stringEquals = str.stringEqual("bar");
        assertTrue(stringEquals.getArguments().contains(str));
        assertTrue(stringEquals.getArguments().contains(Expression.of("bar")));
    }

    @Test
    void getAttr() {
        Expression str = Expression.of("http://example.com");
        ParseUrl parseUrl = str.parseUrl();
        GetAttr getAttr1 = parseUrl.getAttr("scheme");
        assertTrue(getAttr1.getArguments().contains(Expression.of("scheme")));

        GetAttr getAttr2 = parseUrl.getAttr(Identifier.of("scheme"));
        assertTrue(getAttr2.getArguments().contains(Expression.of("scheme")));
    }

    @Test
    void isSet() {
        Expression str = Expression.of("foo");
        IsSet isSet = str.isSet();
        assertTrue(isSet.getArguments().contains(str));
    }

    @Test
    void isValidHostLabel() {
        Expression str = Expression.of("foo");
        IsValidHostLabel isValidHostLabel = str.isValidHostLabel(true);
        assertTrue(isValidHostLabel.getArguments().contains(str));
        assertTrue(isValidHostLabel.getArguments().contains(Expression.of(true)));
    }

    @Test
    void not() {
        Expression bool = Expression.of(true);
        Not not = bool.not();
        assertTrue(not.getArguments().contains(bool));
    }

    @Test
    void parseUrl() {
        Expression str = Expression.of("http://example.com");
        ParseUrl parseUrl = str.parseUrl();
        assertTrue(parseUrl.getArguments().contains(str));
    }

    @Test
    void substring() {
        Expression str = Expression.of("foo");
        Substring substring = str.substring(1, 2, false);
        assertTrue(substring.getArguments().contains(str));
        assertTrue(substring.getArguments().contains(Expression.of(1)));
        assertTrue(substring.getArguments().contains(Expression.of(2)));
        assertTrue(substring.getArguments().contains(Expression.of(false)));

    }
}

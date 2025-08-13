/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Substring;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.UriEncode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;

public final class TestHelpers {

    private TestHelpers() {}

    public static LibraryFunction isSet(String paramName) {
        return IsSet.ofExpressions(Expression.getReference(Identifier.of(paramName)));
    }

    public static LibraryFunction stringEquals(String paramName, String value) {
        return StringEquals.ofExpressions(
                Expression.getReference(Identifier.of(paramName)),
                StringLiteral.of(value));
    }

    public static LibraryFunction stringEquals(Expression expr1, Expression expr2) {
        return StringEquals.ofExpressions(expr1, expr2);
    }

    public static LibraryFunction booleanEquals(String paramName, boolean value) {
        return BooleanEquals.ofExpressions(
                Expression.getReference(Identifier.of(paramName)),
                Literal.booleanLiteral(value));
    }

    public static LibraryFunction booleanEquals(Expression expr, boolean value) {
        return BooleanEquals.ofExpressions(expr, Literal.booleanLiteral(value));
    }

    public static LibraryFunction parseUrl(String paramName) {
        return ParseUrl.ofExpressions(Expression.getReference(Identifier.of(paramName)));
    }

    public static LibraryFunction getAttr(Expression expr, String path) {
        return GetAttr.ofExpressions(expr, Literal.of(path));
    }

    public static LibraryFunction getAttr(String paramName, String path) {
        return GetAttr.ofExpressions(
                Expression.getReference(Identifier.of(paramName)),
                Literal.of(path));
    }

    public static LibraryFunction substring(String paramName, int start, int stop, boolean reverse) {
        return Substring.ofExpressions(
                Expression.getReference(Identifier.of(paramName)),
                Literal.of(start),
                Literal.of(stop),
                Literal.of(reverse));
    }

    public static LibraryFunction substring(Expression expr, int start, int stop, boolean reverse) {
        return Substring.ofExpressions(
                expr,
                Literal.of(start),
                Literal.of(stop),
                Literal.of(reverse));
    }

    public static LibraryFunction not(Expression expr) {
        return Not.ofExpressions(expr);
    }

    public static LibraryFunction not(LibraryFunction fn) {
        return Not.ofExpressions(fn);
    }

    public static LibraryFunction isValidHostLabel(String paramName, boolean allowDots) {
        return IsValidHostLabel.ofExpressions(
                Expression.getReference(Identifier.of(paramName)),
                Literal.of(allowDots));
    }

    public static LibraryFunction isValidHostLabel(Expression expr, boolean allowDots) {
        return IsValidHostLabel.ofExpressions(expr, Literal.of(allowDots));
    }

    public static LibraryFunction uriEncode(String paramName) {
        return UriEncode.ofExpressions(Expression.getReference(Identifier.of(paramName)));
    }

    public static LibraryFunction uriEncode(Expression expr) {
        return UriEncode.ofExpressions(expr);
    }

    public static Endpoint endpoint(String url) {
        return Endpoint.builder().url(Expression.of(url)).build();
    }

    public static Endpoint endpoint(Expression url) {
        return Endpoint.builder().url(url).build();
    }
}

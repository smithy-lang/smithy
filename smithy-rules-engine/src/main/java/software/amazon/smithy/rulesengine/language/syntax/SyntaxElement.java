/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax;

import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Substring;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * A class that is coercible into {@link Condition}s and {@link Expression}s for
 * use in composing rule-sets in code.
 */
@SmithyInternalApi
public abstract class SyntaxElement implements ToCondition, ToExpression {
    /**
     * Returns a BooleanEquals expression comparing this expression to the provided boolean value.
     *
     * @param value the value to compare against.
     * @return the {@link BooleanEquals} function.
     */
    public final BooleanEquals booleanEqual(boolean value) {
        return BooleanEquals.ofExpressions(toExpression(), Expression.of(value));
    }

    /**
     * Returns a StringEquals function of this expression and the given string value.
     *
     * @param value the string value to compare this expression to.
     * @return the {@link StringEquals} function.
     */
    public final StringEquals stringEqual(String value) {
        return StringEquals.ofExpressions(toExpression(), Expression.of(value));
    }

    /**
     * Returns a GetAttr function containing the given path string.
     *
     * @param path the path.
     * @return the {@link GetAttr} function.
     */
    public final GetAttr getAttr(String path) {
        return GetAttr.ofExpressions(toExpression(), path);
    }

    /**
     * Returns a GetAttr function containing the given identifier.
     *
     * @param path the path.
     * @return the {@link GetAttr} function.
     */
    public final GetAttr getAttr(Identifier path) {
        return GetAttr.ofExpressions(toExpression(), path.toString());
    }

    /**
     * Returns an IsSet expression for this instance.
     *
     * @return the {@link IsSet} function.
     */
    public final IsSet isSet() {
        return IsSet.ofExpressions(toExpression());
    }

    /**
     * Returns an isValidHostLabel expression of this expression.
     *
     * @param allowDots whether the UTF-8 {@code .} is considered valid within a host label.
     * @return the {@link IsValidHostLabel} function.
     */
    public final IsValidHostLabel isValidHostLabel(boolean allowDots) {
        return IsValidHostLabel.ofExpressions(toExpression(), allowDots);
    }

    /**
     * Returns a Not expression of this instance.
     *
     * @return the {@link Not} function.
     */
    public final Not not() {
        return Not.ofExpressions(toExpression());
    }

    /**
     * Returns a parseUrl expression of this expression.
     *
     * @return the {@link ParseUrl} function.
     */
    public final ParseUrl parseUrl() {
        return ParseUrl.ofExpressions(toExpression());
    }

    /**
     * Returns a Substring expression of this expression.
     *
     * @param startIndex the starting index of the string.
     * @param stopIndex  the ending index of the string.
     * @param reverse    whether the indexing is should start from end of the string to start.
     * @return the {@link Substring} function.
     */
    public final Substring substring(int startIndex, int stopIndex, boolean reverse) {
        return Substring.ofExpressions(toExpression(), startIndex, stopIndex, reverse);
    }

    /**
     * Converts this expression to a string template.
     * By default, this implementation returns a {@link RuntimeException}.
     *
     * @return the String template.
     */
    public String template() {
        throw new RuntimeException(String.format("cannot convert %s to a string template", this));
    }
}

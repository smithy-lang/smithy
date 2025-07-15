/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
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

    public static LibraryFunction booleanEquals(String paramName, boolean value) {
        return BooleanEquals.ofExpressions(
                Expression.getReference(Identifier.of(paramName)),
                Literal.booleanLiteral(value));
    }

    public static LibraryFunction parseUrl(String urlTemplate) {
        return ParseUrl.ofExpressions(Literal.stringLiteral(Template.fromString(urlTemplate)));
    }

    public static Endpoint endpoint(String url) {
        return Endpoint.builder().url(Expression.of(url)).build();
    }
}

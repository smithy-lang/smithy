/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.visit;

import java.util.List;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.expr.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expr.Ref;
import software.amazon.smithy.rulesengine.language.syntax.fn.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.fn.Fn;
import software.amazon.smithy.rulesengine.language.syntax.fn.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.fn.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.fn.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.syntax.fn.Not;
import software.amazon.smithy.rulesengine.language.syntax.fn.ParseArn;
import software.amazon.smithy.rulesengine.language.syntax.fn.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.fn.PartitionFn;
import software.amazon.smithy.rulesengine.language.syntax.fn.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.fn.Substring;
import software.amazon.smithy.rulesengine.language.syntax.fn.UriEncode;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public abstract class DefaultVisitor<R> implements RuleValueVisitor<R>, ExprVisitor<R>, FnVisitor<R> {
    public abstract R getDefault();

    @Override
    public R visitLiteral(Literal literal) {
        return getDefault();
    }

    @Override
    public R visitRef(Ref ref) {
        return getDefault();
    }

    @Override
    public R visitFn(Fn fn) {
        return getDefault();
    }

    @Override
    public R visitPartition(PartitionFn fn) {
        return getDefault();
    }

    @Override
    public R visitParseArn(ParseArn fn) {
        return getDefault();
    }


    @Override
    public R visitIsValidHostLabel(IsValidHostLabel fn) {
        return getDefault();
    }

    @Override
    public R visitBoolEquals(BooleanEquals fn) {
        return getDefault();
    }

    @Override
    public R visitStringEquals(StringEquals fn) {
        return getDefault();
    }

    @Override
    public R visitIsSet(IsSet fn) {
        return getDefault();
    }

    @Override
    public R visitNot(Not not) {
        return getDefault();
    }

    @Override
    public R visitGetAttr(GetAttr getAttr) {
        return getDefault();
    }

    @Override
    public R visitParseUrl(ParseUrl parseUrl) {
        return getDefault();
    }

    @Override
    public R visitSubstring(Substring substring) {
        return getDefault();
    }

    @Override
    public R visitUriEncode(UriEncode fn) {
        return getDefault();
    }

    @Override
    public R visitTreeRule(List<Rule> rules) {
        return getDefault();
    }

    @Override
    public R visitErrorRule(Expr error) {
        return getDefault();
    }

    @Override
    public R visitEndpointRule(Endpoint endpoint) {
        return getDefault();
    }

}

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

package software.amazon.smithy.rulesengine.reterminus.visit;

import java.util.List;
import software.amazon.smithy.rulesengine.reterminus.Endpoint;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Expr;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Literal;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Ref;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.BooleanEquals;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.Fn;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.GetAttr;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.IsSet;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.IsValidHostLabel;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.Not;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.ParseArn;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.ParseUrl;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.PartitionFn;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.StringEquals;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.Substring;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.UriEncode;
import software.amazon.smithy.rulesengine.reterminus.lang.rule.Rule;

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

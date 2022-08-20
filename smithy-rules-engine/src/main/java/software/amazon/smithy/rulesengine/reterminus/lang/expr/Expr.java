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

package software.amazon.smithy.rulesengine.reterminus.lang.expr;

import static software.amazon.smithy.rulesengine.reterminus.error.RuleError.ctx;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.reterminus.error.InnerParseError;
import software.amazon.smithy.rulesengine.reterminus.eval.Eval;
import software.amazon.smithy.rulesengine.reterminus.eval.Scope;
import software.amazon.smithy.rulesengine.reterminus.eval.Type;
import software.amazon.smithy.rulesengine.reterminus.eval.Typecheck;
import software.amazon.smithy.rulesengine.reterminus.lang.Identifier;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.BooleanEquals;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.FnNode;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.GetAttr;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.IsSet;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.IsValidHostLabel;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.Not;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.ParseArn;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.ParseUrl;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.StringEquals;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.Substring;
import software.amazon.smithy.rulesengine.reterminus.visit.ExprVisitor;

public abstract class Expr implements FromSourceLocation, Typecheck, Eval, ToNode {
    private final SourceLocation sourceLocation;

    private Type cachedType;

    public Expr(SourceLocation sourceLocation) {
        this.sourceLocation = Objects.requireNonNull(sourceLocation);
    }

    public static Expr of(boolean value) {
        return Literal.bool(value);
    }

    public static Expr of(int value) {
        return Literal.integer(value);
    }

    public static Literal of(String value) {
        return literal(StringNode.from(value));
    }

    public static Expr fromNode(Node node) {
        if (node.asObjectNode().isPresent()) {
            ObjectNode on = node.asObjectNode().get();
            Optional<Node> ref = on.getMember("ref");
            Optional<Node> fn = on.getMember("fn");
            if ((ref.isPresent() ? 1 : 0) + (fn.isPresent() ? 1 : 0) != 1) {
                throw new SourceException("expected exactly one of `ref` or `fn` to be set, found "
                                          + Node.printJson(node), node);
            }
            if (ref.isPresent()) {
                return ref(Identifier.of(ref.get().expectStringNode("ref must be a string")), ref.get());
            }
            return ctx("while parsing fn", node, () -> FnNode.fromNode(on).validate());
        } else {
            return Literal.fromNode(node);
        }
    }

    /**
     * Parse a value from a "short form" used within a template.
     *
     * @param shortForm the shortform value
     * @return the parsed expression
     */
    public static Expr parseShortform(String shortForm, FromSourceLocation context) {
        return ctx("while parsing `" + shortForm + "` within a template", context, () -> {
            if (shortForm.contains("#")) {
                String[] parts = shortForm.split("#", 2);
                String base = parts[0];
                String pattern = parts[1];
                return GetAttr.builder(context)
                        .target(ref(Identifier.of(base), context))
                        .path(pattern).build();
            } else {
                return Expr.ref(Identifier.of(shortForm), context);
            }
        });
    }

    public static Ref ref(Identifier name, FromSourceLocation context) {
        return new Ref(name, context);
    }

    public static Literal literal(StringNode node) {
        return Literal.str(new Template(node));
    }

    public SourceLocation getSourceLocation() {
        return this.sourceLocation;
    }

    public abstract <R> R accept(ExprVisitor<R> visitor);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    public GetAttr getAttr(String path) {
        return GetAttr.builder(this).target(this).path(path).build();
    }

    public GetAttr getAttr(Identifier path) {
        return GetAttr.builder(this).target(this).path(path.asString()).build();
    }

    @Override
    public Type typecheck(Scope<Type> scope) {
        // TODO: Could remove all typecheckLocal functions (maybe?)
        Type t = ctx(
                String.format("while typechecking %s", this),
                this,
                () -> typecheckLocal(scope)
        );
        assert cachedType == null || t.equals(cachedType);
        cachedType = t;
        return cachedType;
    }

    public Type type() {
        if (cachedType == null) {
            throw new RuntimeException("you must call typecheck first");
        }
        return cachedType;
    }

    protected abstract Type typecheckLocal(Scope<Type> scope) throws InnerParseError;

    public IsSet isSet() {
        return IsSet.ofExpr(this);
    }

    public BooleanEquals eq(boolean value) {
        return BooleanEquals.ofExprs(this, Expr.of(value));
    }

    public Not not() {
        return Not.ofExpr(this);
    }

    public StringEquals eq(String value) {
        return StringEquals.ofExprs(this, Expr.of(value));
    }

    public ParseArn parseArn() {
        return ParseArn.ofExprs(this);
    }

    public Substring substring(int startIndex, int stopIndex, Boolean reverse) {
        return Substring.ofExprs(this, startIndex, stopIndex, reverse);
    }

    public IsValidHostLabel isValidHostLabel(boolean allowDots) {
        return IsValidHostLabel.ofExprs(this, allowDots);
    }

    public ParseUrl parseUrl() {
        return ParseUrl.ofExprs(this);
    }

    public String template() {
        throw new RuntimeException(String.format("cannot convert %s to a string template", this));
    }
}

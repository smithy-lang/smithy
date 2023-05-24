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

package software.amazon.smithy.rulesengine.language.syntax.expressions;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.TypeCheck;
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.rulesengine.language.stdlib.AwsIsVirtualHostableS3Bucket;
import software.amazon.smithy.rulesengine.language.stdlib.BooleanEquals;
import software.amazon.smithy.rulesengine.language.stdlib.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.stdlib.ParseArn;
import software.amazon.smithy.rulesengine.language.stdlib.ParseUrl;
import software.amazon.smithy.rulesengine.language.stdlib.StringEquals;
import software.amazon.smithy.rulesengine.language.stdlib.Substring;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.functions.Function;
import software.amazon.smithy.rulesengine.language.syntax.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.functions.Not;
import software.amazon.smithy.rulesengine.language.visit.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A dynamically computed expression.
 * <p>
 * Expressions are the fundamental building block of the rule language.
 */
@SmithyUnstableApi
public abstract class Expression implements FromSourceLocation, ToNode, TypeCheck {
    private final SourceLocation sourceLocation;
    private Type cachedType;

    public Expression(SourceLocation sourceLocation) {
        this.sourceLocation = Objects.requireNonNull(sourceLocation);
    }

    public static Expression of(boolean value) {
        return Literal.booleanLiteral(value);
    }

    /**
     * Construct an integer literal for the given value, and returns it as an expression.
     *
     * @param value the integer value.
     * @return the integer value as a literal expression.
     */
    public static Expression of(int value) {
        return Literal.integerLiteral(value);
    }

    /**
     * Constructs a string literal for the given values.
     *
     * @param value the string value.
     * @return the string value as a literal.
     */
    public static Literal of(String value) {
        return getLiteral(StringNode.from(value));
    }

    /**
     * Constructs an expression from the provided {@link Node}.
     *
     * @param node the node to construct the expression from.
     * @return the expression.
     */
    public static Expression fromNode(Node node) {
        if (node.asObjectNode().isPresent()) {
            ObjectNode on = node.asObjectNode().get();
            Optional<Node> ref = on.getMember("ref");
            Optional<Node> fn = on.getMember("fn");
            if ((ref.isPresent() ? 1 : 0) + (fn.isPresent() ? 1 : 0) != 1) {
                throw new SourceException("expected exactly one of `ref` or `fn` to be set, found "
                                          + Node.printJson(node), node);
            }
            if (ref.isPresent()) {
                return getReference(Identifier.of(ref.get().expectStringNode("ref must be a string")), ref.get());
            }
            return context("while parsing fn", node, () -> FunctionNode.fromNode(on).validate());
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
    public static Expression parseShortform(String shortForm, FromSourceLocation context) {
        return context("while parsing `" + shortForm + "` within a template", context, () -> {
            if (shortForm.contains("#")) {
                String[] parts = shortForm.split("#", 2);
                String base = parts[0];
                String pattern = parts[1];
                return GetAttr.builder()
                        .sourceLocation(context)
                        .target(getReference(Identifier.of(base), context))
                        .path(pattern).build();
            } else {
                return Expression.getReference(Identifier.of(shortForm), context);
            }
        });
    }

    /**
     * Constructs a {@link Reference} for the given {@link Identifier} at the given location.
     *
     * @param name    the referenced identifier.
     * @param context the source location.
     * @return the reference.
     */
    public static Reference getReference(Identifier name, FromSourceLocation context) {
        return new Reference(name, context);
    }

    /**
     * Constructs a {@link Literal} from the given {@link StringNode}.
     *
     * @param node the node to construct the literal from.
     * @return the string node as a literal.
     */
    public static Literal getLiteral(StringNode node) {
        return Literal.stringLiteral(new Template(node));
    }

    /**
     * Invoke the {@link ExpressionVisitor} functions for this expression.
     *
     * @param visitor the visitor to be invoked.
     * @param <R>     the visitor return type.
     * @return the return value of the visitor.
     */
    public abstract <R> R accept(ExpressionVisitor<R> visitor);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Constructs a {@link GetAttr} expression containing the given path string.
     *
     * @param path the path.
     * @return the {@link GetAttr} expression.
     */
    public GetAttr getAttr(String path) {
        return GetAttr.builder()
                .sourceLocation(this)
                .target(this).path(path).build();
    }

    /**
     * Constructs a {@link GetAttr} expression containing the given {@link Identifier}.
     *
     * @param path the path {@link Identifier}.
     * @return the {@link GetAttr} expression.
     */
    public GetAttr getAttr(Identifier path) {
        return GetAttr.builder()
                .sourceLocation(this)
                .target(this).path(path.toString()).build();
    }

    @Override
    public Type typeCheck(Scope<Type> scope) {
        // TODO: Could remove all typecheckLocal functions (maybe?)
        Type t = context(
                String.format("while typechecking %s", this),
                this,
                () -> typeCheckLocal(scope)
        );

        if (cachedType != null && !t.equals(cachedType)) {
            throw new RuntimeException(String.format("Checking type `%s` that doesn't match cached type `%s`",
                    t, cachedType));
        }

        cachedType = t;
        return cachedType;
    }

    /**
     * Returns the type for this expression, throws a runtime exception if {@code typeCheck} has not been invoked.
     *
     * @return the type.
     */
    public Type type() {
        if (cachedType == null) {
            throw new RuntimeException("typechecking was never invoked on this expression.");
        }
        return cachedType;
    }

    protected abstract Type typeCheckLocal(Scope<Type> scope) throws InnerParseError;

    /**
     * Returns an IsSet expression for this instance.
     *
     * @return the IsSet expression.
     */
    public IsSet isSet() {
        return IsSet.ofExpression(this);
    }

    /**
     * Returns a {@link BooleanEquals} expression comparing this expression to the provided boolean value.
     *
     * @param value the value to compare against.
     * @return the BooleanEquals {@link Function}.
     */
    public Function equal(boolean value) {
        return BooleanEquals.ofExpressions(this, Expression.of(value));
    }

    /**
     * Returns a Not expression of this instance.
     *
     * @return the {@link Not} expression.
     */
    public Not not() {
        return Not.ofExpression(this);
    }

    /**
     * Returns a StringEquals function of this expression and the given string value.
     *
     * @param value the string value to compare this expression to.
     * @return the StringEquals {@link Function}.
     */
    public Function equal(String value) {
        return StringEquals.ofExpressions(this, Expression.of(value));
    }

    /**
     * Returns a ParseArn function of this expression.
     *
     * @return the ParseArn function expression.
     */
    public Function parseArn() {
        return ParseArn.ofExpression(this);
    }

    /**
     * Returns a substring expression of this expression.
     *
     * @param startIndex the starting index of the string.
     * @param stopIndex  the ending index of the string.
     * @param reverse    whether the indexing is should start from end of the string to start.
     * @return the Substring function expression.
     */
    public Function substring(int startIndex, int stopIndex, Boolean reverse) {
        return Substring.ofExpression(this, startIndex, stopIndex, reverse);
    }

    /**
     * Returns a isValidHostLabel expression of this expression.
     *
     * @param allowDots whether the UTF-8 {@code .} is considered valid within a host label.
     * @return the isValidHostLabel function expression.
     */
    public Function isValidHostLabel(boolean allowDots) {
        return IsValidHostLabel.ofExpression(this, allowDots);
    }

    /**
     * Returns a isVirtualHostableS3Bucket expression of this expression.
     *
     * @param allowDots whether the UTF-8 {@code .} is considered valid within a host label.
     * @return the isVirtualHostableS3Bucket function expression.
     */
    public Function isVirtualHostableS3Bucket(boolean allowDots) {
        return AwsIsVirtualHostableS3Bucket.ofExpression(this, allowDots);
    }

    /**
     * Returns a parseUrl expression of this expression.
     *
     * @return the parseUrl function expression.
     */
    public Function parseUrl() {
        return ParseUrl.ofExpression(this);
    }

    /**
     * Converts this expression to a string template. By default this implementation returns a {@link RuntimeException}.
     *
     * @return the string template.
     */
    public String getTemplate() {
        throw new RuntimeException(String.format("cannot convert %s to a string template", this));
    }
}

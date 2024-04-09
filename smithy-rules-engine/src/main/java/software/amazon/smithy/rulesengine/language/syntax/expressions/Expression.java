/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.TypeCheck;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.SyntaxElement;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A dynamically computed expression.
 * <p>
 * Expressions are the fundamental building block of the rule language.
 */
@SmithyUnstableApi
public abstract class Expression extends SyntaxElement implements FromSourceLocation, ToNode, TypeCheck {
    private final SourceLocation sourceLocation;
    private Type cachedType;

    public Expression(SourceLocation sourceLocation) {
        this.sourceLocation = Objects.requireNonNull(sourceLocation);
    }

    public static Literal of(boolean value) {
        return Literal.booleanLiteral(value);
    }

    /**
     * Construct an integer literal for the given value, and returns it as an expression.
     *
     * @param value the integer value.
     * @return the integer value as a literal expression.
     */
    public static Literal of(int value) {
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
            return context("while parsing fn", node, () -> FunctionNode.fromNode(on).createFunction());
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
                return GetAttr.getDefinition().createFunction(FunctionNode.ofExpressions(GetAttr.ID, context,
                        getReference(Identifier.of(parts[0]), context), of(parts[1])));
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

    protected abstract Type typeCheckLocal(Scope<Type> scope) throws InnerParseError;

    /**
     * Returns the type for this expression, throws a runtime exception if {@code typeCheck} has not been invoked.
     *
     * @return the type.
     */
    public Type type() {
        if (cachedType == null) {
            throw new RuntimeException("Typechecking was never invoked on this expression.");
        }
        return cachedType;
    }

    @Override
    public Condition.Builder toConditionBuilder() {
        return Condition.builder().fn(this);
    }

    @Override
    public Expression toExpression() {
        return this;
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Type typeCheck(Scope<Type> scope) {
        Type type = context(String.format("while typechecking %s", this), this, () -> typeCheckLocal(scope));

        if (cachedType != null && !type.equals(cachedType)) {
            throw new RuntimeException(String.format("Checking type `%s` that doesn't match cached type `%s`",
                    type, cachedType));
        }

        cachedType = type;
        return cachedType;
    }
}

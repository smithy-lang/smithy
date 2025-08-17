/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.RulesVersion;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.OptionalType;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A coalesce function that returns the first non-empty value.
 * At runtime, returns the left value unless it's EmptyValue, in which case returns the right value.
 *
 * <p>Type checking rules:
 * <ul>
 * <li>{@code coalesce(T, T) => T} (same types)</li>
 * <li>{@code coalesce(Optional<T>, T) => T} (unwraps optional)</li>
 * <li>{@code coalesce(T, Optional<T>) => T} (unwraps optional)</li>
 * <li>{@code coalesce(Optional<T>, Optional<T>) => Optional<T>}</li>
 * </ul>
 *
 * <p>Supports chaining:
 * {@code coalesce(opt1, coalesce(opt2, coalesce(opt3, default)))}
 *
 * <p>Available since: rules engine 1.1.
 */
@SmithyUnstableApi
public final class Coalesce extends LibraryFunction {
    public static final String ID = "coalesce";
    private static final Definition DEFINITION = new Definition();

    private Coalesce(FunctionNode functionNode) {
        super(DEFINITION, functionNode);
    }

    /**
     * Gets the {@link FunctionDefinition} implementation.
     *
     * @return the function definition.
     */
    public static Definition getDefinition() {
        return DEFINITION;
    }

    /**
     * Creates a {@link Coalesce} function from the given expressions.
     *
     * @param arg1 the first expression, typically optional.
     * @param arg2 the second expression, used as fallback.
     * @return The resulting {@link Coalesce} function.
     */
    public static Coalesce ofExpressions(ToExpression arg1, ToExpression arg2) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1, arg2));
    }

    @Override
    public RulesVersion availableSince() {
        return RulesVersion.V1_1;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        List<Expression> args = getArguments();
        return visitor.visitCoalesce(args.get(0), args.get(1));
    }

    @Override
    public Type typeCheck(Scope<Type> scope) {
        List<Expression> args = getArguments();

        if (args.size() != 2) {
            throw new IllegalArgumentException("Coalesce requires exactly 2 arguments, got " + args.size());
        }

        Type leftType = args.get(0).typeCheck(scope);
        Type rightType = args.get(1).typeCheck(scope);
        Type leftInner = getInnerType(leftType);
        Type rightInner = getInnerType(rightType);

        // Both must be the same type (after unwrapping optionals)
        if (!leftInner.equals(rightInner)) {
            throw new IllegalArgumentException(String.format(
                    "Type mismatch in coalesce: %s and %s must be the same type",
                    leftType,
                    rightType));
        }

        // Only return Optional if both sides are optional
        if (leftType instanceof OptionalType && rightType instanceof OptionalType) {
            return Type.optionalType(leftInner);
        }

        return leftInner;
    }

    private static Type getInnerType(Type t) {
        return (t instanceof OptionalType) ? ((OptionalType) t).inner() : t;
    }

    /**
     * A {@link FunctionDefinition} for the {@link Coalesce} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Arrays.asList(Type.anyType(), Type.anyType());
        }

        @Override
        public Type getReturnType() {
            return Type.anyType();
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            throw new UnsupportedOperationException("Coalesce evaluation is handled by ExpressionVisitor");
        }

        @Override
        public Coalesce createFunction(FunctionNode functionNode) {
            return new Coalesce(functionNode);
        }
    }
}

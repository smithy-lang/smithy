/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
 *
 * <p>This variadic function requires two or more arguments. At runtime, returns the first argument that contains a
 * non-EmptyValue, otherwise returns the result of the last argument.
 *
 * <p>Type checking rules:
 * <ul>
 * <li>{@code coalesce(T, T, T) => T} (same types)</li>
 * <li>{@code coalesce(Optional<T>, T, T) => T} (any non-optional makes result non-optional)</li>
 * <li>{@code coalesce(Optional<T>, Optional<T>, Optional<T>) => Optional<T>} (all optional)</li>
 * </ul>
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
     * Creates a {@link Coalesce} function from variadic expressions.
     *
     * @param args the expressions to coalesce
     * @return The resulting {@link Coalesce} function.
     */
    public static Coalesce ofExpressions(ToExpression... args) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, args));
    }

    /**
     * Creates a {@link Coalesce} function from a list of expressions.
     *
     * @param args the expressions to coalesce
     * @return The resulting {@link Coalesce} function.
     */
    public static Coalesce ofExpressions(List<? extends ToExpression> args) {
        return ofExpressions(args.toArray(new ToExpression[0]));
    }

    @Override
    public RulesVersion availableSince() {
        return RulesVersion.V1_1;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitCoalesce(getArguments());
    }

    @Override
    public Type typeCheck(Scope<Type> scope) {
        List<Expression> args = getArguments();
        if (args.size() < 2) {
            throw new IllegalArgumentException("Coalesce requires at least 2 arguments, got " + args.size());
        }

        // Get the first argument's type as the baseline
        Type firstType = args.get(0).typeCheck(scope);
        Type baseInnerType = getInnerType(firstType);
        boolean hasNonOptional = !(firstType instanceof OptionalType);

        // Check all other arguments match the base type
        for (int i = 1; i < args.size(); i++) {
            Type argType = args.get(i).typeCheck(scope);
            Type innerType = getInnerType(argType);

            if (!innerType.equals(baseInnerType)) {
                throw new IllegalArgumentException(String.format(
                        "Type mismatch in coalesce at argument %d: expected %s but got %s",
                        i + 1,
                        baseInnerType,
                        innerType));
            }

            hasNonOptional = hasNonOptional || !(argType instanceof OptionalType);
        }

        return hasNonOptional ? baseInnerType : Type.optionalType(baseInnerType);
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
        public int getCost() {
            return 10;
        }

        @Override
        public List<Type> getArguments() {
            return Collections.emptyList();
        }

        @Override
        public Optional<Type> getVariadicArguments() {
            return Optional.of(Type.anyType());
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

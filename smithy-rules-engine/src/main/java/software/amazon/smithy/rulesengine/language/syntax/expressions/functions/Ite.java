/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.RulesVersion;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.OptionalType;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An if-then-else (ITE) function that returns one of two values based on a boolean condition.
 *
 * <p>This function is critical for avoiding SSA (Static Single Assignment) fragmentation in BDD compilation.
 * By computing conditional values atomically without branching, it prevents the graph explosion that occurs when
 * boolean flags like UseFips or UseDualStack create divergent paths with distinct variable identities.
 *
 * <p>Semantics: {@code ite(condition, trueValue, falseValue)}
 * <ul>
 *   <li>If condition is true, returns trueValue</li>
 *   <li>If condition is false, returns falseValue</li>
 *   <li>The condition must be a non-optional boolean (use coalesce to provide a default if needed)</li>
 * </ul>
 *
 * <p>Type checking rules (least upper bound of nullability):
 * <ul>
 *   <li>{@code ite(Boolean, T, T) => T} - both non-optional, result is non-optional</li>
 *   <li>{@code ite(Boolean, T, Optional<T>) => Optional<T>} - any optional makes result optional</li>
 *   <li>{@code ite(Boolean, Optional<T>, T) => Optional<T>} - any optional makes result optional</li>
 *   <li>{@code ite(Boolean, Optional<T>, Optional<T>) => Optional<T>} - both optional, result is optional</li>
 * </ul>
 *
 * <p>Available since: rules engine 1.1.
 */
@SmithyUnstableApi
public final class Ite extends LibraryFunction {
    public static final String ID = "ite";
    private static final Definition DEFINITION = new Definition();

    private Ite(FunctionNode functionNode) {
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
     * Creates a {@link Ite} function from the given expressions.
     *
     * @param condition the boolean condition to evaluate
     * @param trueValue the value to return if condition is true
     * @param falseValue the value to return if condition is false
     * @return The resulting {@link Ite} function.
     */
    public static Ite ofExpressions(ToExpression condition, ToExpression trueValue, ToExpression falseValue) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, condition, trueValue, falseValue));
    }

    /**
     * Creates a {@link Ite} function with a reference condition and string values.
     *
     * @param conditionRef the reference to a boolean parameter
     * @param trueValue the string value if condition is true
     * @param falseValue the string value if condition is false
     * @return The resulting {@link Ite} function.
     */
    public static Ite ofStrings(ToExpression conditionRef, String trueValue, String falseValue) {
        return ofExpressions(conditionRef, Expression.of(trueValue), Expression.of(falseValue));
    }

    @Override
    public RulesVersion availableSince() {
        return RulesVersion.V1_1;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitIte(getArguments().get(0), getArguments().get(1), getArguments().get(2));
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) throws InnerParseError {
        List<Expression> args = getArguments();
        if (args.size() != 3) {
            throw new InnerParseError("ITE requires exactly 3 arguments, got " + args.size());
        }

        // Check condition is a boolean (non-optional)
        Type conditionType = args.get(0).typeCheck(scope);
        if (!conditionType.equals(Type.booleanType())) {
            throw new InnerParseError(String.format(
                    "ITE condition must be a non-optional Boolean, got %s. "
                            + "Use coalesce to provide a default for optional booleans.",
                    conditionType));
        }

        // Get trueValue and falseValue types
        Type trueType = args.get(1).typeCheck(scope);
        Type falseType = args.get(2).typeCheck(scope);

        // Extract base types (unwrap Optional if present)
        Type trueBaseType = getInnerType(trueType);
        Type falseBaseType = getInnerType(falseType);

        // Base types must match
        if (!trueBaseType.equals(falseBaseType)) {
            throw new InnerParseError(String.format(
                    "ITE branches must have the same base type: true branch is %s, false branch is %s",
                    trueBaseType,
                    falseBaseType));
        }

        // Result is optional if EITHER branch is optional (least upper bound)
        boolean resultIsOptional = (trueType instanceof OptionalType) || (falseType instanceof OptionalType);
        return resultIsOptional ? Type.optionalType(trueBaseType) : trueBaseType;
    }

    private static Type getInnerType(Type t) {
        return (t instanceof OptionalType) ? ((OptionalType) t).inner() : t;
    }

    /**
     * A {@link FunctionDefinition} for the {@link Ite} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            // Actual type checking is done in typeCheck override
            return Arrays.asList(Type.booleanType(), Type.anyType(), Type.anyType());
        }

        @Override
        public Type getReturnType() {
            // Actual return type is computed in typeCheck override
            return Type.anyType();
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            throw new UnsupportedOperationException("ITE evaluation is handled by ExpressionVisitor");
        }

        @Override
        public Ite createFunction(FunctionNode functionNode) {
            return new Ite(functionNode);
        }

        @Override
        public int getCost() {
            return 10;
        }
    }
}

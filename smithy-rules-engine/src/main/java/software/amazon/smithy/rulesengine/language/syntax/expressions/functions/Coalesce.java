/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.AnyType;
import software.amazon.smithy.rulesengine.language.evaluation.type.OptionalType;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A coalesce function that returns the first non-empty value, with type-safe fallback handling.
 * At runtime, returns the left value unless it's EmptyValue, in which case returns the right value.
 *
 * <p>Type checking rules:
 * <ul>
 * <li>{@code coalesce(T, T) => T} (same types)</li>
 * <li>{@code coalesce(T, AnyType) => T} (AnyType adapts to concrete type)</li>
 * <li>{@code coalesce(AnyType, T) => T} (AnyType adapts to concrete type)</li>
 * <li>{@code coalesce(T, S) => S} (if T.isA(S), i.e., S is more general)</li>
 * <li>{@code coalesce(T, S) => T} (if S.isA(T), i.e., T is more general)</li>
 * <li>{@code coalesce(Optional<T>, S) => common_type(T, S)} (unwraps optional)</li>
 * <li>{@code coalesce(T, Optional<S>) => common_type(T, S)} (unwraps optional)</li>
 * <li>{@code coalesce(Optional<T>, Optional<S>) => Optional<common_type(T, S)>}</li>
 * </ul>
 *
 * <p>Special handling for AnyType: Since AnyType can masquerade as any type, when coalescing
 * with a concrete type, the concrete type is used as the result type.
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
    public String availableSince() {
        return "1.1";
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        List<Expression> args = getArguments();
        return visitor.visitCoalesce(args.get(0), args.get(1));
    }

    // Type checking rules for coalesce:
    //
    // This function returns the first non-empty value with type-safe fallback handling.
    // The type resolution follows these rules:
    //
    // 1. If both types are identical, use that type
    // 2. Special handling for AnyType: Since AnyType.isA() always returns true (it can masquerade as any type), we
    //    need to handle it specially. When coalescing AnyType with a concrete type, we use the concrete type as the
    //    result, since AnyType can adapt to it at runtime.
    // 3. For other types, we use the isA relationship to find the more general type:
    //    - If left.isA(right), then right is more general, use right
    //    - If right.isA(left), then left is more general, use left
    // 4. If no type relationship exists, throw a type mismatch error
    //
    // The result is wrapped in Optional only if BOTH inputs are Optional, since coalesce(optional, required)
    // guarantees a non-empty result.
    //
    // Examples:
    // - coalesce(String, String) => String
    // - coalesce(Optional<String>, String) => String
    // - coalesce(Optional<String>, Optional<String>) => Optional<String>
    // - coalesce(String, AnyType) => String (AnyType adapts)
    // - coalesce(SubType, SuperType) => SuperType (more general)
    @Override
    public Type typeCheck(Scope<Type> scope) {
        List<Expression> args = getArguments();

        if (args.size() != 2) {
            throw new IllegalArgumentException("Coalesce requires exactly 2 arguments, got " + args.size());
        }

        Type leftType = args.get(0).typeCheck(scope);
        Type rightType = args.get(1).typeCheck(scope);

        // Find the least upper bound (most specific common type)
        Type resultType = lubForCoalesce(leftType, rightType);

        // Only return Optional if both sides can be empty
        if (leftType instanceof OptionalType && rightType instanceof OptionalType) {
            return Type.optionalType(resultType);
        }

        return resultType;
    }

    // Finds the least upper bound (LUB) for coalesce type checking.
    // The LUB is the most specific type that both input types can be assigned to.
    // Special handling for AnyType: it adapts to concrete types rather than dominating them.
    private static Type lubForCoalesce(Type a, Type b) {
        Type ai = getInnerType(a);
        Type bi = getInnerType(b);

        if (ai.equals(bi)) {
            return ai;
        } else if (ai instanceof AnyType) {
            return bi; // AnyType adapts to concrete type
        } else if (bi instanceof AnyType) {
            return ai; // AnyType adapts to concrete type
        } else if (ai.isA(bi)) {
            return bi; // bi is more general
        } else if (bi.isA(ai)) {
            return ai; // ai is more general
        }

        throw new IllegalArgumentException("Type mismatch in coalesce: " + a + " and " + b + " have no common type");
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

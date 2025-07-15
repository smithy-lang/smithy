/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-engine function for checking whether a string is a valid DNS host label.
 */
@SmithyUnstableApi
public final class IsValidHostLabel extends LibraryFunction {
    public static final String ID = "isValidHostLabel";
    private static final Definition DEFINITION = new Definition();

    IsValidHostLabel(FunctionNode functionNode) {
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
     * Creates a {@link IsValidHostLabel} function from the given expressions.
     *
     * @param arg1 the value to check.
     * @param arg2 whether to allow subdomains.
     * @return The resulting {@link IsValidHostLabel} function.
     */
    public static IsValidHostLabel ofExpressions(ToExpression arg1, ToExpression arg2) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1, arg2));
    }

    /**
     * Creates a {@link IsValidHostLabel} function from the given expressions.
     *
     * @param arg1 the value to check.
     * @param arg2 whether to allow subdomains.
     * @return The resulting {@link IsValidHostLabel} function.
     */
    public static IsValidHostLabel ofExpressions(ToExpression arg1, boolean arg2) {
        return ofExpressions(arg1, Expression.of(arg2));
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    /**
     * A {@link FunctionDefinition} for the {@link IsValidHostLabel} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Arrays.asList(Type.stringType(), Type.booleanType());
        }

        @Override
        public Type getReturnType() {
            return Type.booleanType();
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            String hostLabel = arguments.get(0).expectStringValue().getValue();
            boolean allowDots = arguments.get(1).expectBooleanValue().getValue();
            return Value.booleanValue(isValidHostLabel(hostLabel, allowDots));
        }

        @Override
        public IsValidHostLabel createFunction(FunctionNode functionNode) {
            return new IsValidHostLabel(functionNode);
        }

        @Override
        public int getCostHeuristic() {
            return 8;
        }
    }

    /**
     * Check if a hostLabel is valid.
     *
     * @param hostLabel Host label to check.
     * @param allowDots Set to true to allow dots as separators.
     * @return true if the label is valid.
     */
    public static boolean isValidHostLabel(String hostLabel, boolean allowDots) {
        int len = hostLabel == null ? 0 : hostLabel.length();
        if (len == 0) {
            return false;
        }

        // Single-label mode
        if (!allowDots) {
            return isValidSingleLabel(hostLabel, 0, len);
        }

        // Multi-label mode
        int start = 0;
        for (int i = 0; i <= len; i++) {
            if (i == len || hostLabel.charAt(i) == '.') {
                // chunk is hostLabel[start..i)
                int chunkLen = i - start;
                if (chunkLen < 1 || chunkLen > 63) {
                    return false;
                } else if (!isValidSingleLabel(hostLabel, start, i)) {
                    return false;
                }
                start = i + 1;
            }
        }
        return true;
    }

    // Validates a single label in s[start..end): ^[A-Za-z0-9][A-Za-z0-9\-]{0,62}$
    private static boolean isValidSingleLabel(String s, int start, int end) {
        int length = end - start;
        if (length < 1 || length > 63) {
            return false;
        }

        // first char must be [A-Za-z0-9]
        if (!isAlphanumeric(s.charAt(start))) {
            return false;
        }

        // remaining chars must be [A-Za-z0-9-]
        for (int i = start + 1; i < end; i++) {
            char c = s.charAt(i);
            if (!isAlphanumeric(c) && c != '-') {
                return false;
            }
        }

        return true;
    }

    private static boolean isAlphanumeric(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
}

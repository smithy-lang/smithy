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

        private boolean isValidHostLabel(String hostLabel, boolean allowDots) {
            if (allowDots) {
                // ensure that empty matches at the end are included
                for (String subLabel : hostLabel.split("[.]", -1)) {
                    if (!isValidHostLabel(subLabel, false)) {
                        return false;
                    }
                }
                return true;
            } else {
                return hostLabel.matches("[a-zA-Z\\d][a-zA-Z\\d\\-]{1,62}");
            }
        }

        @Override
        public IsValidHostLabel createFunction(FunctionNode functionNode) {
            return new IsValidHostLabel(functionNode);
        }
    }
}

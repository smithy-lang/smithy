/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for determining whether a reference parameter is set.
 */
@SmithyUnstableApi
public final class IsSet extends LibraryFunction {
    public static final String ID = "isSet";
    private static final Definition DEFINITION = new Definition();

    private IsSet(FunctionNode functionNode) {
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
     * Creates a {@link IsSet} function from the given expressions.
     *
     * @param arg1 the expression to negate.
     * @return The resulting {@link IsSet} function.
     */
    public static IsSet ofExpressions(ToExpression arg1) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1));
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitIsSet(expectOneArgument());
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) {
        Expression arg = expectOneArgument();
        Type type = arg.typeCheck(scope);
        if (!type.isA(Type.optionalType(Type.anyType()))) {
            throw new RuntimeException(String.format("Expected %s but found %s",
                    Type.optionalType(Type.anyType()),
                    type));
        }

        // Insert the non-null fact, but only for references.
        if (arg instanceof Reference) {
            scope.setNonNull((Reference) arg);
        }
        return Type.booleanType();
    }

    /**
     * A {@link FunctionDefinition} for the {@link IsSet} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Collections.singletonList(Type.optionalType(Type.anyType()));
        }

        @Override
        public Type getReturnType() {
            return Type.booleanType();
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            // Specialized in the ExpressionVisitor, so this doesn't need an implementation.
            return null;
        }

        @Override
        public IsSet createFunction(FunctionNode functionNode) {
            return new IsSet(functionNode);
        }
    }
}

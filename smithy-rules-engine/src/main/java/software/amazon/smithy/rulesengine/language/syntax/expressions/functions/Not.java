/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class Not extends LibraryFunction {
    public static final String ID = "not";
    private static final Definition DEFINITION = new Definition();

    private Not(FunctionNode functionNode) {
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
     * Creates a {@link Not} function from the given expressions.
     *
     * @param arg1 the expression to negate.
     * @return The resulting {@link Not} function.
     */
    public static Not ofExpressions(ToExpression arg1) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1));
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitNot(expectOneArgument());
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) {
        // Not must be typechecked in an interior scope because information doesn't flow back out of `not`.
        return scope.inScope(() -> context("while typechecking `not`",
                this,
                () -> expectOneArgument().typeCheck(scope).expectBooleanType()));
    }

    /**
     * A {@link FunctionDefinition} for the {@link Not} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Collections.singletonList(Type.booleanType());
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
        public Not createFunction(FunctionNode functionNode) {
            return new Not(functionNode);
        }
    }
}

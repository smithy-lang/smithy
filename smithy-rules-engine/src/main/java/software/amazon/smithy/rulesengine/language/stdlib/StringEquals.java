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

package software.amazon.smithy.rulesengine.language.stdlib;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.fn.Function;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.fn.LibraryFunction;
import software.amazon.smithy.rulesengine.language.visit.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for comparing strings for equality.
 */
@SmithyUnstableApi
public final class StringEquals extends Function {
    public static final String ID = "stringEquals";
    private static final Definition DEFINITION = new Definition();

    public StringEquals(FunctionNode functionNode) {
        super(functionNode);
    }

    /**
     * Constructs a function that compare the left and right expressions for string equality.
     *
     * @param left  the left expression.
     * @param right the right expression.
     * @return a function instance representing the StringEquals comparison.
     */
    public static StringEquals ofExpressions(Expression left, Expression right) {
        return new StringEquals(FunctionNode.ofExpressions(DEFINITION.getId(), left, right));
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitStringEquals(functionNode.getArguments().get(0), functionNode.getArguments().get(1));
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) throws InnerParseError {
        LibraryFunction.checkTypeSignature(DEFINITION.getArguments(), functionNode.getArguments(), scope);
        return DEFINITION.getReturnType();
    }

    private static class Definition extends FunctionDefinition {
        public static final String ID = StringEquals.ID;

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Arrays.asList(Type.string(), Type.string());
        }

        @Override
        public Type getReturnType() {
            return Type.bool();
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            return Value.bool(arguments.get(0).expectString().equals(arguments.get(1).expectString()));
        }
    }
}

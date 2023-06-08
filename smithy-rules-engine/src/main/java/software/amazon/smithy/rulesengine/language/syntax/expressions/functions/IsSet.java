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

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
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

    public IsSet(FunctionNode functionNode) {
        super(DEFINITION, functionNode);
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
                    Type.optionalType(Type.anyType()), type));
        }

        // Insert the non-null fact, but only for references.
        if (arg instanceof Reference) {
            scope.setNonNull((Reference) arg);
        }
        return Type.booleanType();
    }

    public static final class Definition implements FunctionDefinition {
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

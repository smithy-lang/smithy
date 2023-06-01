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

package software.amazon.smithy.rulesengine.language.syntax.functions;

import java.util.List;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.visitors.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * A function ({@link Function}) which is constructed from a {@link FunctionDefinition}.
 */
@SmithyUnstableApi
public class LibraryFunction extends Function {
    private final FunctionDefinition definition;
    private final FunctionNode functionNode;

    public LibraryFunction(FunctionDefinition definition, FunctionNode functionNode) {
        super(functionNode);
        this.definition = definition;
        this.functionNode = functionNode;
    }

    /**
     * Creates a new {@link LibraryFunction} instance with the given arguments.
     *
     * @param definition the function definition.
     * @param arguments  the function arguments.
     * @return the {@link LibraryFunction} instance.
     */
    public static LibraryFunction ofExpressions(FunctionDefinition definition, Expression... arguments) {
        FunctionNode node = FunctionNode.ofExpressions(definition.getId(), arguments);
        return new LibraryFunction(definition, node);
    }

    public static void checkTypeSignature(List<Type> expectedArgs, List<Expression> actualArguments, Scope<Type> scope)
            throws InnerParseError {
        if (expectedArgs.size() != actualArguments.size()) {
            throw new InnerParseError(
                    String.format(
                            "Expected %s arguments but found %s",
                            expectedArgs.size(),
                            actualArguments)
            );
        }
        for (int i = 0; i < expectedArgs.size(); i++) {
            Type expected = expectedArgs.get(i);
            Type actual = actualArguments.get(i).typeCheck(scope);
            if (!expected.equals(actual)) {
                Type optAny = Type.optionalType(Type.anyType());
                String hint = "";
                if (actual.isA(optAny) && !expected.isA(optAny)
                        && actual.expectOptionalType().inner().equals(expected)) {
                    hint = String.format(
                            "hint: use `assign` in a condition or `isSet(%s)` to prove that this value is non-null",
                            actualArguments.get(i));
                    hint = StringUtils.indent(hint, 2);
                }
                throw new InnerParseError(
                        String.format(
                                "Unexpected type in the %s argument: Expected %s but found %s%n%s",
                                ordinal(i + 1), expected, actual, hint)
                );
            }
        }

    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) {
        RuleError.context(String.format("while typechecking the invocation of %s", definition.getId()), this, () -> {
            try {
                checkTypeSignature(definition.getArguments(), functionNode.getArguments(), scope);
            } catch (InnerParseError e) {
                throw new RuntimeException(e);
            }
        });
        return definition.getReturnType();
    }

    private static String ordinal(int arg) {
        switch (arg) {
            case 1:
                return "first";
            case 2:
                return "second";
            case 3:
                return "third";
            case 4:
                return "fourth";
            case 5:
                return "fifth";
            default:
                return String.format("%s", arg);
        }
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(this.definition, this.functionNode.getArguments());
    }
}

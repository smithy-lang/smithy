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

package software.amazon.smithy.rulesengine.language.syntax.fn;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;

import java.util.List;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.util.StringUtils;
import software.amazon.smithy.rulesengine.language.visit.ExprVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A function ({@link Fn}) which is constructed from a {@link FunctionDefinition}.
 */
@SmithyUnstableApi
public class LibraryFunction extends Fn {
    private final FunctionDefinition definition;
    private final FnNode fnNode;

    public LibraryFunction(FunctionDefinition definition, FnNode fnNode) {
        super(fnNode);
        this.definition = definition;
        this.fnNode = fnNode;
    }

    public static LibraryFunction ofExprs(FunctionDefinition defn, Expr... expr) {
        FnNode node = FnNode.ofExprs(defn.id(), expr);
        return new LibraryFunction(defn, node);
    }

    public static void checkTypeSignature(List<Type> expectedArgs, List<Expr> actualArguments, Scope<Type> scope)
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
            Type actual = actualArguments.get(i).typecheck(scope);
            if (!expected.equals(actual)) {
                Type optAny = Type.optional(new Type.Any());
                String hint = "";
                if (actual.isA(optAny) && !expected.isA(optAny) && actual.expectOptional().inner().equals(expected)) {
                    hint = String.format(
                            "%nhint: use `assign` in a condition or `isSet(%s)` to prove that this value is non-null",
                            actualArguments.get(i));
                    hint = StringUtils.indent(hint, 2);
                }
                throw new InnerParseError(
                        String.format(
                                "Unexpected type in the %s argument: Expected %s but found %s %s",
                                ordinal(i + 1), expected, actual, hint)
                );
            }
        }

    }

    @Override
    protected Type typecheckLocal(Scope<Type> scope) {
        ctx(String.format("while typechecking the invocation of %s", definition.id()), this, () -> {
            try {
                checkTypeSignature(definition.arguments(), fnNode.getArgv(), scope);
            } catch (InnerParseError e) {
                throw new RuntimeException(e);
            }
        });
        return definition.returnType();
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
    public <T> T accept(ExprVisitor<T> visitor) {
        return visitor.visitLibraryFunction(this.definition, this.fnNode.getArgv());
    }
}

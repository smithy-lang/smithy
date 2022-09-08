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

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.util.StringUtils;
import software.amazon.smithy.rulesengine.language.visit.FnVisitor;

public class StandardLibraryFunction extends Fn {
    private final FunctionDefinition definition;
    private final FnNode fnNode;

    public StandardLibraryFunction(FunctionDefinition definition, FnNode fnNode) {
        super(fnNode);
        this.definition = definition;
        this.fnNode = fnNode;
    }

    public static StandardLibraryFunction ofExprs(FunctionDefinition defn, Expr... expr) {
        FnNode node = FnNode.ofExprs(defn.id(), expr);
        return new StandardLibraryFunction(defn, node);
    }

    @Override
    public Value eval(Scope<Value> scope) {
        List<Value> args = fnNode.getArgv().stream().map(expr -> expr.eval(scope)).collect(Collectors.toList());
        return definition.eval(args);
    }


    @Override
    protected Type typecheckLocal(Scope<Type> scope) throws InnerParseError {
        List<Type> expectedArgs = definition.arguments();
        if (fnNode.getArgv().size() != expectedArgs.size()) {
            throw new InnerParseError(
                    String.format(
                            "Expected %s arguments but found %s",
                            expectedArgs.size(),
                            fnNode.getArgv().size())
            );
        }
        for (int i = 0; i < expectedArgs.size(); i++) {
            Type expected = expectedArgs.get(i);
            Type actual = fnNode.getArgv().get(i).typecheck(scope);
            if (!expected.equals(actual)) {
                Type optAny = Type.optional(new Type.Any());
                String hint = "";
                if (actual.isA(optAny) && !expected.isA(optAny) && actual.expectOptional().inner().equals(expected)) {
                    hint = String.format("\nhint: use `assign` in a condition or `isSet(%s)` to prove that this value is non-null", fnNode.getArgv().get(i));
                    hint = StringUtils.indent(hint, 2);
                }
                throw new InnerParseError(
                        String.format(
                                "Unexpected type in the %s argument of `%s`: Expected %s but found %s %s",
                                ordinal(i + 1), definition.id(), expected, actual, hint)
                );
            }
        }
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
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitGenericFunction(this);
    }
}

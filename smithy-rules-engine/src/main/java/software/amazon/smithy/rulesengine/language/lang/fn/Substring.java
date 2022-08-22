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

package software.amazon.smithy.rulesengine.language.lang.fn;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;

import java.util.List;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.lang.Identifier;
import software.amazon.smithy.rulesengine.language.lang.expr.Expr;
import software.amazon.smithy.rulesengine.language.visit.FnVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public class Substring extends VarargFn {
    public static final String ID = "substring";
    public static final Identifier SUBSTRING = Identifier.of("substring");
    private static final int EXPECTED_NUMBER_ARGS = 4;


    public Substring(FnNode fnNode) {
        super(fnNode);
    }

    public static Substring ofExprs(Expr expr, int startIndex, int stopIndex, Boolean reverse) {
        return new Substring(FnNode.ofExprs(ID, expr, Expr.of(startIndex), Expr.of(stopIndex), Expr.of(reverse)));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitSubstring(this);
    }

    public Expr stringToParse() {
        return expectVariableArgs(EXPECTED_NUMBER_ARGS).get(0);
    }

    public Expr startIndex() {
        return expectVariableArgs(EXPECTED_NUMBER_ARGS).get(1);
    }

    public Expr stopIndex() {
        return expectVariableArgs(EXPECTED_NUMBER_ARGS).get(2);
    }

    public Expr reverse() {
        return expectVariableArgs(EXPECTED_NUMBER_ARGS).get(3);
    }


    @Override
    public Value eval(Scope<Value> scope) {
        List<Expr> args = expectVariableArgs(EXPECTED_NUMBER_ARGS);
        String str = args.get(0).eval(scope).expectString();
        int startIndex = args.get(1).eval(scope).expectInt();
        int stopIndex = args.get(2).eval(scope).expectInt();
        boolean reverse = args.get(3).eval(scope).expectBool();

        if (startIndex >= stopIndex || str.length() < stopIndex) {
            return new Value.None();
        }

        if (!reverse) {
            return Value.str(str.substring(startIndex, stopIndex));
        } else {
            int revStart = str.length() - stopIndex;
            int revStop = str.length() - startIndex;
            return Value.str(str.substring(revStart, revStop));
        }
    }


    @Override
    protected Type typecheckLocal(Scope<Type> scope) {
        return ctx(
                "while typechecking substring",
                this,
                () -> {
                    stringToParse().typecheck(scope).expectString();
                    startIndex().typecheck(scope).expectInt();
                    stopIndex().typecheck(scope).expectInt();
                    reverse().typecheck(scope).expectBool();
                    return Type.optional(Type.str());
                }
        );
    }


}

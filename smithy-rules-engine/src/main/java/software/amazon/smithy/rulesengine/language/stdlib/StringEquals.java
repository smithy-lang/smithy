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
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.fn.Fn;
import software.amazon.smithy.rulesengine.language.syntax.fn.FnNode;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.LibraryFunction;
import software.amazon.smithy.rulesengine.language.visit.ExprVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class StringEquals extends Fn {
    public static final String ID = "stringEquals";
    private static final Defn DEFN = new Defn();

    public StringEquals(FnNode fnNode) {
        super(fnNode);
    }

    public static Fn ofExprs(Expr left, Expr right) {
        return LibraryFunction.ofExprs(DEFN, left, right);
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitStringEquals(fnNode.getArgv().get(0), fnNode.getArgv().get(1));
    }

    @Override
    protected Type typecheckLocal(Scope<Type> scope) throws InnerParseError {
        LibraryFunction.checkTypeSignature(DEFN.arguments(), fnNode.getArgv(), scope);
        return DEFN.returnType();
    }

    private static class Defn extends FunctionDefinition {
        public static final String ID = StringEquals.ID;


        @Override
        public String id() {
            return ID;
        }

        @Override
        public List<Type> arguments() {
            return Arrays.asList(Type.str(), Type.str());
        }

        @Override
        public Type returnType() {
            return Type.bool();
        }

        @Override
        public Value eval(List<Value> arguments) {
            return Value.bool(arguments.get(0).expectString().equals(arguments.get(1).expectString()));
        }
    }
}

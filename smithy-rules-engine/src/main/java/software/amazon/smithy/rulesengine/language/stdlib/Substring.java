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
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.fn.Fn;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.StandardLibraryFunction;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class Substring extends FunctionDefinition {
    public static final String ID = "substring";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Type> arguments() {
        return Arrays.asList(Type.str(), Type.integer(), Type.integer(), Type.bool());
    }

    @Override
    public Type returnType() {
        return Type.str();
    }

    @Override
    public Value eval(List<Value> arguments) {
        String str = arguments.get(0).expectString();
        int startIndex = arguments.get(1).expectInt();
        int stopIndex = arguments.get(2).expectInt();
        boolean reverse = arguments.get(3).expectBool();

        if (!str.chars().allMatch(ch -> ch >= 0 && ch <= 127)) {
            return Value.none();
        }

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

    public static Fn ofExprs(Expr str, int startIndex, int stopIndex, Boolean reverse) {
        return StandardLibraryFunction.ofExprs(
                new Substring(),
                str, Expr.of(startIndex), Expr.of(stopIndex), Expr.of(reverse)
        );
    }
}

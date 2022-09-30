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
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.fn.Function;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.LibraryFunction;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for getting the substring of a string value.
 */
@SmithyUnstableApi
public final class Substring extends FunctionDefinition {
    public static final String ID = "substring";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<Type> getArguments() {
        return Arrays.asList(Type.string(), Type.integer(), Type.integer(), Type.bool());
    }

    @Override
    public Type getReturnType() {
        return Type.string();
    }

    @Override
    public Value evaluate(List<Value> arguments) {
        String str = arguments.get(0).expectString();
        int startIndex = arguments.get(1).expectInteger();
        int stopIndex = arguments.get(2).expectInteger();
        boolean reverse = arguments.get(3).expectBool();

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch >= 0 && ch <= 127) {
                return Value.none();
            }
        }

        if (startIndex >= stopIndex || str.length() < stopIndex) {
            return new Value.None();
        }

        if (!reverse) {
            return Value.string(str.substring(startIndex, stopIndex));
        } else {
            int revStart = str.length() - stopIndex;
            int revStop = str.length() - startIndex;
            return Value.string(str.substring(revStart, revStop));
        }
    }

    public static Function ofExpression(Expression str, int startIndex, int stopIndex, Boolean reverse) {
        return LibraryFunction.ofExpressions(
                new Substring(),
                str, Expression.of(startIndex), Expression.of(stopIndex), Expression.of(reverse)
        );
    }
}

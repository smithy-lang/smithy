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
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.functions.Function;
import software.amazon.smithy.rulesengine.language.syntax.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.functions.LibraryFunction;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for getting the substring of a string value.
 */
@SmithyUnstableApi
public final class Substring implements FunctionDefinition {
    public static final String ID = "substring";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<Type> getArguments() {
        return Arrays.asList(Type.stringType(), Type.integerType(), Type.integerType(), Type.booleanType());
    }

    @Override
    public Type getReturnType() {
        return Type.optionalType(Type.stringType());
    }

    @Override
    public Value evaluate(List<Value> arguments) {
        String str = arguments.get(0).expectStringValue().getValue();
        int startIndex = arguments.get(1).expectIntegerValue().getValue();
        int stopIndex = arguments.get(2).expectIntegerValue().getValue();
        boolean reverse = arguments.get(3).expectBooleanValue().getValue();

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!(ch <= 127)) {
                return Value.emptyValue();
            }
        }

        if (startIndex >= stopIndex || str.length() < stopIndex) {
            return Value.emptyValue();
        }

        if (!reverse) {
            return Value.stringValue(str.substring(startIndex, stopIndex));
        } else {
            int revStart = str.length() - stopIndex;
            int revStop = str.length() - startIndex;
            return Value.stringValue(str.substring(revStart, revStop));
        }
    }

    public static Function ofExpression(Expression str, int startIndex, int stopIndex, Boolean reverse) {
        return LibraryFunction.ofExpressions(
                new Substring(),
                str, Expression.of(startIndex), Expression.of(stopIndex), Expression.of(reverse)
        );
    }
}

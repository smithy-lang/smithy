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
public final class IsValidHostLabel extends FunctionDefinition {
    public static final String ID = "isValidHostLabel";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Type> arguments() {
        return Arrays.asList(Type.str(), Type.bool());
    }

    @Override
    public Type returnType() {
        return Type.bool();
    }

    @Override
    public Value eval(List<Value> arguments) {
        String hostLabel = arguments.get(0).expectString();
        boolean allowDots = arguments.get(1).expectBool();
        if (allowDots) {
            return Value.bool(hostLabel.matches("[a-zA-Z\\d][a-zA-Z\\d\\-.]{0,62}"));
        } else {
            return Value.bool(hostLabel.matches("[a-zA-Z\\d][a-zA-Z\\d\\-]{0,62}"));
        }
    }

    public static Fn ofExprs(Expr input, boolean allowDots) {
        return StandardLibraryFunction.ofExprs(new IsValidHostLabel(), input, Expr.of(allowDots));
    }
}

/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.jmespath;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.jmespath.ast.LiteralExpression;

/**
 * Defines the positional arguments, variadic arguments, and return value
 * of JMESPath functions.
 */
final class FunctionDefinition {

    @FunctionalInterface
    interface ArgValidator {
        String validate(LiteralExpression argument);
    }

    final LiteralExpression returnValue;
    final List<ArgValidator> arguments;
    final ArgValidator variadic;

    FunctionDefinition(LiteralExpression returnValue, ArgValidator... arguments) {
        this(returnValue, Arrays.asList(arguments), null);
    }

    FunctionDefinition(LiteralExpression returnValue, List<ArgValidator> arguments, ArgValidator variadic) {
        this.returnValue = returnValue;
        this.arguments = arguments;
        this.variadic = variadic;
    }

    static ArgValidator isType(RuntimeType type) {
        return arg -> {
            if (type == RuntimeType.ANY || arg.getType() == RuntimeType.ANY) {
                return null;
            } else if (arg.getType() == type) {
                return null;
            } else {
                return "Expected argument to be " + type + ", but found " + arg.getType();
            }
        };
    }

    static ArgValidator listOfType(RuntimeType type) {
        return arg -> {
            if (type == RuntimeType.ANY || arg.getType() == RuntimeType.ANY) {
                return null;
            } else if (arg.getType() == RuntimeType.ARRAY) {
                List<Object> values = arg.expectArrayValue();
                for (int i = 0; i < values.size(); i++) {
                    LiteralExpression element = LiteralExpression.from(values.get(i));
                    if (element.getType() != type) {
                        return "Expected an array of " + type + ", but found " + element.getType() + " at index " + i;
                    }
                }
            } else {
                return "Expected argument to be an array, but found " + arg.getType();
            }
            return null;
        };
    }

    static ArgValidator oneOf(RuntimeType... types) {
        return arg -> {
            if (arg.getType() == RuntimeType.ANY) {
                return null;
            }

            for (RuntimeType type : types) {
                if (arg.getType() == type || type == RuntimeType.ANY) {
                    return null;
                }
            }

            return "Expected one of " + Arrays.toString(types) + ", but found " + arg.getType();
        };
    }
}

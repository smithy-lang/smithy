/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import static software.amazon.smithy.jmespath.ast.LiteralExpression.ANY;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.ARRAY;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.BOOLEAN;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.NUMBER;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.OBJECT;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.STRING;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.jmespath.functions.FunctionArgument;

/**
 * Defines the positional arguments, variadic arguments, and return value
 * of JMESPath functions.
 */
final class FunctionDefinition {

    static final Map<String, FunctionDefinition> FUNCTIONS = new HashMap<>();

    static {
        FunctionDefinition.ArgValidator isAny = isType(RuntimeType.ANY);
        FunctionDefinition.ArgValidator isString = isType(RuntimeType.STRING);
        FunctionDefinition.ArgValidator isNumber = isType(RuntimeType.NUMBER);
        FunctionDefinition.ArgValidator isArray = isType(RuntimeType.ARRAY);

        FUNCTIONS.put("abs", new FunctionDefinition(NUMBER, isNumber));
        FUNCTIONS.put("avg", new FunctionDefinition(NUMBER, listOfType(RuntimeType.NUMBER)));
        FUNCTIONS.put("contains",
                new FunctionDefinition(
                        BOOLEAN,
                        oneOf(RuntimeType.ARRAY, RuntimeType.STRING),
                        isAny));
        FUNCTIONS.put("ceil", new FunctionDefinition(NUMBER, isNumber));
        FUNCTIONS.put("ends_with", new FunctionDefinition(NUMBER, isString, isString));
        FUNCTIONS.put("floor", new FunctionDefinition(NUMBER, isNumber));
        FUNCTIONS.put("join", new FunctionDefinition(STRING, isString, listOfType(RuntimeType.STRING)));
        FUNCTIONS.put("keys", new FunctionDefinition(ARRAY, isType(RuntimeType.OBJECT)));
        FUNCTIONS.put("length",
                new FunctionDefinition(
                        NUMBER,
                        oneOf(RuntimeType.STRING, RuntimeType.ARRAY, RuntimeType.OBJECT)));
        // TODO: Support expression reference return type validation?
        FUNCTIONS.put("map", new FunctionDefinition(ARRAY, isType(RuntimeType.EXPRESSION), isArray));
        // TODO: support array<X|Y>
        FUNCTIONS.put("max", new FunctionDefinition(NUMBER, isArray));
        FUNCTIONS.put("max_by", new FunctionDefinition(NUMBER, isArray, isType(RuntimeType.EXPRESSION)));
        FUNCTIONS.put("merge", new FunctionDefinition(OBJECT, Collections.emptyList(), isType(RuntimeType.OBJECT)));
        FUNCTIONS.put("min", new FunctionDefinition(NUMBER, isArray));
        FUNCTIONS.put("min_by", new FunctionDefinition(NUMBER, isArray, isType(RuntimeType.EXPRESSION)));
        FUNCTIONS.put("not_null", new FunctionDefinition(ANY, Collections.singletonList(isAny), isAny));
        FUNCTIONS.put("reverse", new FunctionDefinition(ARRAY, oneOf(RuntimeType.ARRAY, RuntimeType.STRING)));
        FUNCTIONS.put("sort", new FunctionDefinition(ARRAY, isArray));
        FUNCTIONS.put("sort_by", new FunctionDefinition(ARRAY, isArray, isType(RuntimeType.EXPRESSION)));
        FUNCTIONS.put("starts_with", new FunctionDefinition(BOOLEAN, isString, isString));
        FUNCTIONS.put("sum", new FunctionDefinition(NUMBER, listOfType(RuntimeType.NUMBER)));
        FUNCTIONS.put("to_array", new FunctionDefinition(ARRAY, isAny));
        FUNCTIONS.put("to_string", new FunctionDefinition(STRING, isAny));
        FUNCTIONS.put("to_number", new FunctionDefinition(NUMBER, isAny));
        FUNCTIONS.put("type", new FunctionDefinition(STRING, isAny));
        FUNCTIONS.put("values", new FunctionDefinition(ARRAY, isType(RuntimeType.OBJECT)));
    }

    public static FunctionDefinition from(String string) {
        return FUNCTIONS.get(string);
    }

    @FunctionalInterface
    public interface ArgValidator {
        String validate(LiteralExpression argument);
    }

    public final LiteralExpression returnValue;
    public final List<ArgValidator> arguments;
    public final ArgValidator variadic;

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

    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> arguments) {
        throw new UnsupportedOperationException();
    }
}

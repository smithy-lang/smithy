/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.Set;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;

public interface FunctionArgument<T> {

    T expectValue();

    T expectType(RuntimeType runtimeType);

    T expectAnyOf(Set<RuntimeType> types);

    default T expectString() {
        return expectType(RuntimeType.STRING);
    }

    default T expectNumber() {
        return expectType(RuntimeType.NUMBER);
    }

    default T expectArray() {
        return expectType(RuntimeType.ARRAY);
    }

    default T expectObject() {
        return expectType(RuntimeType.OBJECT);
    }

    default JmespathExpression expectExpression() {
        throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
    }

    static <T> FunctionArgument<T> of(JmespathRuntime<T> runtime, JmespathExpression expression) {
        return new Expression<>(runtime, expression);
    }

    static <T> FunctionArgument<T> of(JmespathRuntime<T> runtime, T value) {
        return new Value<>(runtime, value);
    }

    class Value<T> implements FunctionArgument<T> {
        JmespathRuntime<T> runtime;
        T value;

        public Value(JmespathRuntime<T> runtime, T value) {
            this.runtime = runtime;
            this.value = value;
        }

        @Override
        public T expectValue() {
            return value;
        }

        public T expectType(RuntimeType runtimeType) {
            if (runtime.is(value, runtimeType)) {
                return value;
            } else {
                throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
            }
        }

        public T expectAnyOf(Set<RuntimeType> types) {
            // TODO: Handle abstract runtimes with a chained ifThenElse
            // OR have abstract implementations of functions check types inline instead
            if (types.contains(runtime.typeOf(value))) {
                return value;
            } else {
                throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
            }
        }
    }

    class Expression<T> implements FunctionArgument<T> {
        JmespathRuntime<T> runtime;
        JmespathExpression expression;

        public Expression(JmespathRuntime<T> runtime, JmespathExpression expression) {
            this.runtime = runtime;
            this.expression = expression;
        }

        @Override
        public T expectValue() {
            return runtime.createError(JmespathExceptionType.INVALID_TYPE, "invalid-type");
        }

        @Override
        public T expectType(RuntimeType runtimeType) {
            return runtime.createError(JmespathExceptionType.INVALID_TYPE, "invalid-type");
        }

        @Override
        public T expectAnyOf(Set<RuntimeType> types) {
            return runtime.createError(JmespathExceptionType.INVALID_TYPE, "invalid-type");
        }

        @Override
        public JmespathExpression expectExpression() {
            return expression;
        }
    }

}

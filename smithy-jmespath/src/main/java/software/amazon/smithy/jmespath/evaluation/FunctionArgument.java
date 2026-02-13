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

    default T expectValue() {
        throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
    }

    default T expectString() {
        throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
    }

    default T expectNumber() {
        throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
    }

    default T expectArray() {
        throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
    }

    default T expectObject() {
        throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
    }

    default T expectAnyOf(Set<RuntimeType> types) {
        throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
    }

    default JmespathExpression expectExpression() {
        throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
    }

    static <T> FunctionArgument<T> of(JmespathExpression expression) {
        return new Expression<>( expression);
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

        protected T expectType(RuntimeType runtimeType) {
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

        @Override
        public T expectString() {
            return expectType(RuntimeType.STRING);
        }

        @Override
        public T expectNumber() {
            return expectType(RuntimeType.NUMBER);
        }

        @Override
        public T expectArray() {
            return expectType(RuntimeType.ARRAY);
        }

        @Override
        public T expectObject() {
            return expectType(RuntimeType.OBJECT);
        }
    }

    class Expression<T> implements FunctionArgument<T> {
        JmespathExpression expression;

        public Expression(JmespathExpression expression) {
            this.expression = expression;
        }

        @Override
        public JmespathExpression expectExpression() {
            return expression;
        }
    }

}

package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.Set;

public abstract class FunctionArgument<T> {

    protected final JmespathRuntime<T> runtime;

    protected FunctionArgument(JmespathRuntime<T> runtime) {
        this.runtime = runtime;
    }

    public T expectValue() {
        throw new JmespathException("invalid-type");
    }

    public T expectString() {
        throw new JmespathException("invalid-type");
    }

    public T expectNumber() {
        throw new JmespathException("invalid-type");
    }

    public T expectObject() {
        throw new JmespathException("invalid-type");
    }

    public T expectAnyOf(Set<RuntimeType> types) {
        throw new JmespathException("invalid-type");
    }

    public JmespathExpression expectExpression() {
        throw new JmespathException("invalid-type");
    }

    public static <T> FunctionArgument<T> of(JmespathRuntime<T> runtime, JmespathExpression expression) {
        return new Expression<T>(runtime, expression);
    }

    public static <T> FunctionArgument<T> of(JmespathRuntime<T> runtime, T value) {
        return new Value<T>(runtime, value);
    }

    static class Value<T> extends FunctionArgument<T> {
        T value;

        public Value(JmespathRuntime<T> runtime, T value) {
            super(runtime);
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
                throw new JmespathException("invalid-type");
            }
        }

        public T expectAnyOf(Set<RuntimeType> types) {
            if (types.contains(runtime.typeOf(value))) {
                return value;
            } else {
                throw new JmespathException("invalid-type");
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
        public T expectObject() {
            return expectType(RuntimeType.OBJECT);
        }
    }

    static class Expression<T> extends FunctionArgument<T> {
        JmespathExpression expression;

        public Expression(JmespathRuntime<T> runtime, JmespathExpression expression) {
            super(runtime);
            this.expression = expression;
        }

        @Override
        public JmespathExpression expectExpression() {
            return expression;
        }
    }


}


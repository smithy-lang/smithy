package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.Runtime;

public abstract class FunctionArgument<T> {

    protected final Runtime<T> runtime;

    protected FunctionArgument(Runtime<T> runtime) {
        this.runtime = runtime;
    }

    public abstract T expectString();

    public abstract T expectNumber();

    public abstract JmespathExpression expectExpression();

    public static <T> FunctionArgument<T> of(Runtime<T> runtime, JmespathExpression expression) {
        return new Expression<T>(runtime, expression);
    }

    public static <T> FunctionArgument<T> of(Runtime<T> runtime, T value) {
        return new Value<T>(runtime, value);
    }

    static class Value<T> extends FunctionArgument<T> {
        T value;

        public Value(Runtime<T> runtime, T value) {
            super(runtime);
            this.value = value;
        }

        @Override
        public T expectString() {
            if (runtime.is(value, RuntimeType.STRING)) {
                return value;
            } else {
                throw new JmespathException("invalid-type");
            }
        }

        @Override
        public T expectNumber() {
            if (runtime.is(value, RuntimeType.NUMBER)) {
                return value;
            } else {
                throw new JmespathException("invalid-type");
            }
        }

        @Override
        public JmespathExpression expectExpression() {
            // TODO: Check spec, tests, etc
            throw new JmespathException("invalid-type");
        }
    }

    static class Expression<T> extends FunctionArgument<T> {
        JmespathExpression expression;

        public Expression(Runtime<T> runtime, JmespathExpression expression) {
            super(runtime);
            this.expression = expression;
        }

        @Override
        public T expectString() {
            // TODO: Check spec, tests, etc
            throw new JmespathException("invalid-type");
        }

        @Override
        public T expectNumber() {
            // TODO: Check spec, tests, etc
            throw new JmespathException("invalid-type");
        }

        @Override
        public JmespathExpression expectExpression() {
            return expression;
        }
    }


}


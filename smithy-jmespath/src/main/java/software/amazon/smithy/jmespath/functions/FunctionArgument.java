package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.JmespathExpression;

public interface FunctionArgument<T> {

    public T expectValue();

    public JmespathExpression expectExpression();

    static <T> FunctionArgument<T> of(JmespathExpression expression) {
        return new Expression<T>(expression);
    }

    static <T> FunctionArgument<T> of(T value) {
        return new Value<T>(value);
    }

    static class Value<T> implements FunctionArgument<T> {
        T value;

        public Value(T value) {
            this.value = value;
        }

        @Override
        public T expectValue() {
            return value;
        }

        @Override
        public JmespathExpression expectExpression() {
            // TODO: Check spec, tests, etc
            throw new IllegalStateException();
        }
    }

    static class Expression<T> implements FunctionArgument<T> {
        JmespathExpression expression;

        public Expression(JmespathExpression expression) {
            this.expression = expression;
        }

        @Override
        public T expectValue() {
            // TODO: Check spec, tests, etc
            throw new IllegalStateException();
        }

        @Override
        public JmespathExpression expectExpression() {
            return null;
        }
    }


}


package software.amazon.smithy.jmespath;

import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.evaluation.NumberType;
import software.amazon.smithy.jmespath.evaluation.Runtime;
import software.amazon.smithy.jmespath.evaluation.EvaluationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// TODO: Or "TypeCheckerRuntime"
public class LiteralExpressionRuntime implements Runtime<LiteralExpression> {

    // TODO: Add problems, or make a separate TypeCheckerRuntime that subclasses this

    @Override
    public RuntimeType typeOf(LiteralExpression value) {
        return value.getType();
    }

    @Override
    public LiteralExpression createNull() {
        return LiteralExpression.NULL;
    }

    @Override
    public LiteralExpression createBoolean(boolean b) {
        return LiteralExpression.from(b);
    }

    @Override
    public boolean toBoolean(LiteralExpression value) {
        return value.expectBooleanValue();
    }

    @Override
    public LiteralExpression createString(String string) {
        return LiteralExpression.from(string);
    }

    @Override
    public String toString(LiteralExpression value) {
        return value.expectStringValue();
    }

    @Override
    public LiteralExpression createNumber(Number value) {
        return LiteralExpression.from(value);
    }

    @Override
    public NumberType numberType(LiteralExpression value) {
        return EvaluationUtils.numberType(value.expectNumberValue());
    }

    @Override
    public Number toNumber(LiteralExpression value) {
        return value.expectNumberValue();
    }

    @Override
    public Number length(LiteralExpression value) {
        switch (value.getType()) {
            case STRING: return EvaluationUtils.codePointCount(value.expectStringValue());
            case ARRAY: return value.expectArrayValue().size();
            case OBJECT: return value.expectObjectValue().size();
            default: throw new IllegalStateException();
        }
    }

    @Override
    public LiteralExpression element(LiteralExpression array, LiteralExpression index) {
        return LiteralExpression.from(array.expectArrayValue().get(index.expectNumberValue().intValue()));
    }

    @Override
    public Iterable<LiteralExpression> iterate(LiteralExpression array) {
        switch (array.getType()) {
            case ARRAY: return new WrappingIterable(array.expectArrayValue());
            case OBJECT: return new WrappingIterable(array.expectObjectValue().keySet());
            default: throw new IllegalStateException("invalid-type");
        }
    }

    private static class WrappingIterable implements Iterable<LiteralExpression> {

        private final Iterable<?> inner;

        private WrappingIterable(Iterable<?> inner) {
            this.inner = inner;
        }

        @Override
        public Iterator<LiteralExpression> iterator() {
            return new WrappingIterator(inner.iterator());
        }

        private static class WrappingIterator implements Iterator<LiteralExpression> {

            private final Iterator<?> inner;

            private WrappingIterator(Iterator<?> inner) {
                this.inner = inner;
            }

            @Override
            public boolean hasNext() {
                return inner.hasNext();
            }

            @Override
            public LiteralExpression next() {
                return LiteralExpression.from(inner.next());
            }
        }
    }

    @Override
    public ArrayBuilder<LiteralExpression> arrayBuilder() {
        return new ArrayLiteralExpressionBuilder();
    }

    private static class ArrayLiteralExpressionBuilder implements ArrayBuilder<LiteralExpression> {
        private final List<Object> result = new ArrayList<>();

        @Override
        public void add(LiteralExpression value) {
            result.add(value);
        }

        @Override
        public void addAll(LiteralExpression array) {
            result.addAll(array.expectArrayValue());
        }

        @Override
        public LiteralExpression build() {
            return LiteralExpression.from(result);
        }
    }

    @Override
    public LiteralExpression value(LiteralExpression value, LiteralExpression name) {
        return LiteralExpression.from(value.expectObjectValue().get(name.expectStringValue()));
    }

    @Override
    public LiteralExpression keys(LiteralExpression value) {
        Map<String, Object> map = value.expectObjectValue();
        return LiteralExpression.from(new ArrayList<>(map.keySet()));
    }

    @Override
    public ObjectBuilder<LiteralExpression> objectBuilder() {
        return new ObjectLiteralExpressionBuilder();
    }

    private static class ObjectLiteralExpressionBuilder implements ObjectBuilder<LiteralExpression> {
        private final Map<String, Object> result = new HashMap<>();

        @Override
        public void put(LiteralExpression key, LiteralExpression value) {
            result.put(key.expectStringValue(), value);
        }

        @Override
        public void putAll(LiteralExpression object) {
            result.putAll(object.expectObjectValue());
        }

        @Override
        public LiteralExpression build() {
            return LiteralExpression.from(result);
        }
    }
}

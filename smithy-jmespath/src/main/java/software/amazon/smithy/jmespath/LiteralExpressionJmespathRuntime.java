package software.amazon.smithy.jmespath;

import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.evaluation.NumberType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.jmespath.evaluation.EvaluationUtils;
import software.amazon.smithy.jmespath.evaluation.WrappingIterable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiteralExpressionJmespathRuntime implements JmespathRuntime<LiteralExpression> {

    public static final LiteralExpressionJmespathRuntime INSTANCE = new LiteralExpressionJmespathRuntime();

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
    public boolean asBoolean(LiteralExpression value) {
        return value.expectBooleanValue();
    }

    @Override
    public LiteralExpression createString(String string) {
        return LiteralExpression.from(string);
    }

    @Override
    public String asString(LiteralExpression value) {
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
    public Number asNumber(LiteralExpression value) {
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
    public Iterable<LiteralExpression> toIterable(LiteralExpression array) {
        switch (array.getType()) {
            case ARRAY: return new WrappingIterable<>(LiteralExpression::from, array.expectArrayValue());
            case OBJECT: return new WrappingIterable<>(LiteralExpression::from, array.expectObjectValue().keySet());
            default: throw new IllegalStateException("invalid-type");
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
            if (array.isArrayValue()) {
                result.addAll(array.expectArrayValue());
            } else {
                result.addAll(array.expectObjectValue().keySet());
            }
        }

        @Override
        public LiteralExpression build() {
            return LiteralExpression.from(result);
        }
    }

    @Override
    public LiteralExpression value(LiteralExpression value, LiteralExpression name) {
        if (value.isObjectValue()) {
            return value.getObjectField(name.expectStringValue());
        } else {
            return createNull();
        }
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

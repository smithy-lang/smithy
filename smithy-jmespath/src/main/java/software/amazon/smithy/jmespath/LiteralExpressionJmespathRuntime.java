/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.evaluation.EvaluationUtils;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.jmespath.evaluation.MappingIterable;
import software.amazon.smithy.jmespath.evaluation.NumberType;

/**
 * A singleton implementation of the JmespathRuntime interface based on instances of the LiteralExpression class.
 * <p>
 * Does not use values of the additional LiteralExpression.EXPREF or LiteralExpression.ANY types
 * as they aren't necessary.
 */
public final class LiteralExpressionJmespathRuntime implements JmespathRuntime<LiteralExpression> {

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
    public int length(LiteralExpression value) {
        switch (value.getType()) {
            case STRING:
                return EvaluationUtils.codePointCount(value.expectStringValue());
            case ARRAY:
                return value.expectArrayValue().size();
            case OBJECT:
                return value.expectObjectValue().size();
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public LiteralExpression element(LiteralExpression array, int index) {
        return LiteralExpression.from(array.expectArrayValue().get(index));
    }

    @Override
    public Iterable<LiteralExpression> asIterable(LiteralExpression array) {
        switch (array.getType()) {
            case ARRAY:
                return new MappingIterable<>(LiteralExpression::from, array.expectArrayValue());
            case OBJECT:
                return new MappingIterable<>(LiteralExpression::from, array.expectObjectValue().keySet());
            default:
                throw new IllegalStateException("invalid-type");
        }
    }

    @Override
    public ArrayBuilder<LiteralExpression> arrayBuilder() {
        return new ArrayLiteralExpressionBuilder();
    }

    private static final class ArrayLiteralExpressionBuilder implements ArrayBuilder<LiteralExpression> {
        private final List<Object> result = new ArrayList<>();

        @Override
        public void add(LiteralExpression value) {
            result.add(value.getValue());
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

    private static final class ObjectLiteralExpressionBuilder implements ObjectBuilder<LiteralExpression> {
        private final Map<String, Object> result = new HashMap<>();

        @Override
        public void put(LiteralExpression key, LiteralExpression value) {
            result.put(key.expectStringValue(), value.getValue());
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

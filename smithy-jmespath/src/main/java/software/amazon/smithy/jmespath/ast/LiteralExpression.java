/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;

/**
 * Represents a literal value.
 */
public final class LiteralExpression extends JmespathExpression {

    /** Sentinel value to represent ANY. */
    public static final LiteralExpression ANY = new LiteralExpression(new Object());

    /** Sentinel value to represent any ARRAY. */
    public static final LiteralExpression ARRAY = new LiteralExpression(new ArrayList<>());

    /** Sentinel value to represent any OBJECT. */
    public static final LiteralExpression OBJECT = new LiteralExpression(new HashMap<>());

    /** Sentinel value to represent any BOOLEAN. */
    public static final LiteralExpression BOOLEAN = new LiteralExpression(false);

    /** Sentinel value to represent any STRING. */
    public static final LiteralExpression STRING = new LiteralExpression("");

    /** Sentinel value to represent any NULL. */
    public static final LiteralExpression NUMBER = new LiteralExpression(0);

    /** Sentinel value to represent an expression reference. */
    public static final LiteralExpression EXPREF = new LiteralExpression((Function<Object, Void>) o -> null);

    /** Sentinel value to represent null. */
    public static final LiteralExpression NULL = new LiteralExpression(null);

    private final Object value;

    public LiteralExpression(Object value) {
        this(value, 1, 1);
    }

    public LiteralExpression(Object value, int line, int column) {
        super(line, column);

        // Unwrapped any wrapping that would mess up type checking.
        if (value instanceof LiteralExpression) {
            this.value = ((LiteralExpression) value).getValue();
        } else {
            this.value = value;
        }
    }

    /**
     * Creates a LiteralExpression from {@code value}, unwrapping it if necessary.
     *
     * @param value Value to create the expression from.
     * @return Returns the LiteralExpression of the given {@code value}.
     */
    public static LiteralExpression from(Object value) {
        if (value instanceof LiteralExpression) {
            return (LiteralExpression) value;
        } else {
            return new LiteralExpression(value);
        }
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLiteral(this);
    }

    /**
     * Gets the nullable value contained in the literal value.
     *
     * @return Returns the contained value.
     */
    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof LiteralExpression)) {
            return false;
        } else {
            LiteralExpression other = (LiteralExpression) o;
            // Compare all numbers as doubles to remove conflicts between integers, floats, and doubles.
            if (value instanceof Number && other.getValue() instanceof Number) {
                return ((Number) value).doubleValue() == ((Number) other.getValue()).doubleValue();
            } else {
                return Objects.equals(value, ((LiteralExpression) o).value);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "LiteralExpression{value=" + value + '}';
    }

    /**
     * Gets the type of the value.
     *
     * @return Returns the literal expression's runtime type.
     */
    public RuntimeType getType() {
        if (isArrayValue()) {
            return RuntimeType.ARRAY;
        } else if (isObjectValue()) {
            return RuntimeType.OBJECT;
        } else if (isStringValue()) {
            return RuntimeType.STRING;
        } else if (isBooleanValue()) {
            return RuntimeType.BOOLEAN;
        } else if (isNumberValue()) {
            return RuntimeType.NUMBER;
        } else if (isNullValue()) {
            return RuntimeType.NULL;
        } else if (this == EXPREF) {
            return RuntimeType.EXPRESSION;
        } else {
            return RuntimeType.ANY;
        }
    }

    /**
     * Expects the value to be an object and gets a field by
     * name. If the field does not exist, then a
     * {@link LiteralExpression} with a null value is returned.
     *
     * @param name Field to get from the expected object.
     * @return Returns the object field value.
     */
    public LiteralExpression getObjectField(String name) {
        Map<String, Object> values = expectObjectValue();
        return values.containsKey(name)
                ? new LiteralExpression(values.get(name))
                : new LiteralExpression(null);
    }

    /**
     * Expects the value to be an object and checks if it contains
     * a field by name.
     *
     * @param name Field to get from the expected object.
     * @return Returns true if the object contains the given key.
     */
    public boolean hasObjectField(String name) {
        return expectObjectValue().containsKey(name);
    }

    /**
     * Expects the value to be an array and gets the value at the given
     * index. If the index is negative, it is computed to the array
     * length minus the index. If the computed index does not exist,
     * a {@link LiteralExpression} with a null value is returned.
     *
     * @param index Index to get from the array.
     * @return Returns the array value.
     */
    public LiteralExpression getArrayIndex(int index) {
        List<Object> values = expectArrayValue();

        if (index < 0) {
            index = values.size() + index;
        }

        return index >= 0 && values.size() > index
                ? new LiteralExpression(values.get(index))
                : new LiteralExpression(null);
    }

    /**
     * Checks if the value is a string.
     *
     * @return Returns true if the value is a string.
     */
    public boolean isStringValue() {
        return value instanceof String;
    }

    /**
     * Checks if the value is a number.
     *
     * @return Returns true if the value is a number.
     */
    public boolean isNumberValue() {
        return value instanceof Number;
    }

    /**
     * Checks if the value is a boolean.
     *
     * @return Returns true if the value is a boolean.
     */
    public boolean isBooleanValue() {
        return value instanceof Boolean;
    }

    /**
     * Checks if the value is an array.
     *
     * @return Returns true if the value is an array.
     */
    public boolean isArrayValue() {
        return value instanceof List;
    }

    /**
     * Checks if the value is an object.
     *
     * @return Returns true if the value is an object.
     */
    public boolean isObjectValue() {
        return value instanceof Map;
    }

    /**
     * Checks if the value is null.
     *
     * @return Returns true if the value is null.
     */
    public boolean isNullValue() {
        return value == null;
    }

    /**
     * Gets the value as a string.
     *
     * @return Returns the string value.
     * @throws JmespathException if the value is not a string.
     */
    public String expectStringValue() {
        if (value instanceof String) {
            return (String) value;
        }

        throw new JmespathException("Expected a string literal, but found " + value.getClass());
    }

    /**
     * Gets the value as a number.
     *
     * @return Returns the number value.
     * @throws JmespathException if the value is not a number.
     */
    public Number expectNumberValue() {
        if (value instanceof Number) {
            return (Number) value;
        }

        throw new JmespathException("Expected a number literal, but found " + value.getClass());
    }

    /**
     * Gets the value as a boolean.
     *
     * @return Returns the boolean value.
     * @throws JmespathException if the value is not a boolean.
     */
    public boolean expectBooleanValue() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        throw new JmespathException("Expected a boolean literal, but found " + value.getClass());
    }

    /**
     * Gets the value as an array.
     *
     * @return Returns the array value.
     * @throws JmespathException if the value is not an array.
     */
    @SuppressWarnings("unchecked")
    public List<Object> expectArrayValue() {
        try {
            return (List<Object>) value;
        } catch (ClassCastException e) {
            throw new JmespathException("Expected an array literal, but found " + value.getClass());
        }
    }

    /**
     * Gets the value as an object.
     *
     * @return Returns the object value.
     * @throws JmespathException if the value is not an object.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> expectObjectValue() {
        try {
            return (Map<String, Object>) value;
        } catch (ClassCastException e) {
            throw new JmespathException("Expected a map literal, but found " + value.getClass());
        }
    }

    /**
     * Returns true if the value is truthy according to JMESPath.
     *
     * @return Returns true or false if truthy.
     */
    public boolean isTruthy() {
        switch (getType()) {
            case ANY: // just assume it's true.
            case NUMBER: // number is always true
            case EXPRESSION: // references are always true
                return true;
            case STRING:
                return !expectStringValue().isEmpty();
            case ARRAY:
                return !expectArrayValue().isEmpty();
            case OBJECT:
                return !expectObjectValue().isEmpty();
            case BOOLEAN:
                return expectBooleanValue();
            default:
                return false;
        }
    }
}

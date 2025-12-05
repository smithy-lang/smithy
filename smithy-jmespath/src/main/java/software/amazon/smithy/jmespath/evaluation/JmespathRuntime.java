/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.Collection;
import java.util.Comparator;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;

/**
 * An interface to provide the operations needed for JMESPath expression evaluation
 * based on any runtime representation of JSON values.
 * <p>
 * Several methods have default implementations that are at least correct,
 * but implementors can override them with more efficient implementations.
 */
public interface JmespathRuntime<T> extends Comparator<T> {

    ///////////////////////////////
    // General Operations
    ///////////////////////////////

    /**
     * Returns the basic type of the given value: NULL, BOOLEAN, STRING, NUMBER, OBJECT, or ARRAY.
     * <p>
     * MUST NOT ever return EXPRESSION or ANY.
     */
    RuntimeType typeOf(T value);

    /**
     * Shorthand for {@code typeOf(value).equals(type)}.
     */
    default boolean is(T value, RuntimeType type) {
        return typeOf(value).equals(type);
    }

    /**
     * Returns true iff the given value is truthy according
     * to <a href="https://jmespath.org/specification.html#or-expressions">the JMESPath specification</a>.
     */
    default boolean isTruthy(T value) {
        switch (typeOf(value)) {
            case NULL:
                return false;
            case BOOLEAN:
                return asBoolean(value);
            case STRING:
                return !asString(value).isEmpty();
            case NUMBER:
                return true;
            case ARRAY:
            case OBJECT:
                Iterable<? extends T> iterable = asIterable(value);
                if (iterable instanceof Collection<?>) {
                    return !((Collection<?>) iterable).isEmpty();
                } else {
                    return iterable.iterator().hasNext();
                }
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Returns true iff the two given values are equal.
     * <p>
     * Note that just calling Objects.equals() is generally not correct
     * because it does not consider different Number representations of the same value
     * the same.
     */
    default boolean equal(T a, T b) {
        return EvaluationUtils.equals(this, a, b);
    }

    @Override
    default int compare(T a, T b) {
        if (is(a, RuntimeType.STRING) && is(b, RuntimeType.STRING)) {
            return asString(a).compareTo(asString(b));
        } else if (is(a, RuntimeType.NUMBER) && is(b, RuntimeType.NUMBER)) {
            return EvaluationUtils.compareNumbersWithPromotion(asNumber(a), asNumber(b));
        } else {
            throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "invalid-type");
        }
    }

    /**
     * Returns a JSON string representation of the given value.
     * <p>
     * Note the distinction between this method and asString(),
     * which can be called only on STRINGs and just casts the value to a String.
     */
    default String toString(T value) {
        // Quick and dirty implementation just for test names for now
        switch (typeOf(value)) {
            case NULL:
                return "null";
            case BOOLEAN:
                return asBoolean(value) ? "true" : "false";
            case STRING:
                return '"' + asString(value) + '"';
            case NUMBER:
                return asNumber(value).toString();
            case ARRAY:
                StringBuilder arrayStringBuilder = new StringBuilder();
                arrayStringBuilder.append("[");
                boolean first = true;
                for (T element : asIterable(value)) {
                    if (first) {
                        first = false;
                    } else {
                        arrayStringBuilder.append(",");
                    }
                    arrayStringBuilder.append(toString(element));
                }
                arrayStringBuilder.append("]");
                return arrayStringBuilder.toString();
            case OBJECT:
                StringBuilder objectStringBuilder = new StringBuilder();
                objectStringBuilder.append("{");
                boolean firstKey = true;
                for (T key : asIterable(value)) {
                    if (firstKey) {
                        firstKey = false;
                    } else {
                        objectStringBuilder.append(", ");
                    }
                    objectStringBuilder.append(toString(key));
                    objectStringBuilder.append(": ");
                    objectStringBuilder.append(toString(value(value, key)));
                }
                objectStringBuilder.append("}");
                return objectStringBuilder.toString();
            default:
                throw new IllegalStateException();
        }
    }

    ///////////////////////////////
    // NULLs
    ///////////////////////////////

    /**
     * Returns `null`.
     * <p>
     * Runtimes may or may not use a Java null value to represent a JSON null value.
     */
    T createNull();

    ///////////////////////////////
    // BOOLEANs
    ///////////////////////////////

    T createBoolean(boolean b);

    /**
     * If the given value is a BOOLEAN, return it as a boolean.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    boolean asBoolean(T value);

    ///////////////////////////////
    // STRINGs
    ///////////////////////////////

    T createString(String string);

    /**
     * If the given value is a STRING, return it as a String.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     * <p>
     * Note the distinction between this method and toString(),
     * which can be called on any value and produces a JSON string.
     */
    String asString(T value);

    ///////////////////////////////
    // NUMBERs
    ///////////////////////////////

    T createNumber(Number value);

    /**
     * Returns the type of Number that asNumber() will produce for this value.
     * Will be more efficient for some runtimes than checking the class of asNumber().
     */
    NumberType numberType(T value);

    /**
     * If the given value is a NUMBER, return it as a Number.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    Number asNumber(T value);

    ///////////////////////////////
    // ARRAYs
    ///////////////////////////////

    ArrayBuilder<T> arrayBuilder();

    interface ArrayBuilder<T> {

        void add(T value);

        void addAll(T array);

        T build();
    }

    /**
     * If the given value is an ARRAY, returns the element at the given index.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    T element(T array, T index);

    /**
     * If the given value is an ARRAY, returns the specified slice.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     * <p>
     * The start and stop values will always be non-null and non-negative.
     * Step will always be non-zero.
     * If step is positive, start will be less than or equal to stop.
     * If step is negative, start will be greater than or equal to stop.
     */
    default T slice(T array, Number startNumber, Number stopNumber, Number stepNumber) {
        JmespathRuntime.ArrayBuilder<T> output = arrayBuilder();
        int start = startNumber.intValue();
        int stop = stopNumber.intValue();
        int step = stepNumber.intValue();

        if (start < stop) {
            if (step > 0) {
                for (int idx = start; idx < stop; idx += step) {
                    output.add(element(array, createNumber(idx)));
                }
            }
        } else {
            if (step < 0) {
                // List is iterating in reverse
                for (int idx = start; idx > stop; idx += step) {
                    output.add(element(array, createNumber(idx)));
                }
            }
        }
        return output.build();
    }

    ///////////////////////////////
    // OBJECTs
    ///////////////////////////////

    ObjectBuilder<T> objectBuilder();

    interface ObjectBuilder<T> {

        void put(T key, T value);

        void putAll(T object);

        T build();
    }

    /**
     * If the given value is an OBJECT, returns the value mapped to the given key.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    T value(T object, T key);

    ///////////////////////////////
    // Common collection operations for ARRAYs and OBJECTs
    ///////////////////////////////

    /**
     * Returns the number of elements in an ARRAY or the number of keys in an OBJECT.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    Number length(T value);

    /**
     * Iterate over the elements of an ARRAY or the keys of an OBJECT.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     * <p>
     * Does not use Collection to avoid assuming there are fewer than Integer.MAX_VALUE
     * elements in the array.
     */
    Iterable<? extends T> asIterable(T value);
}

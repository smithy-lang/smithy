/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;

public interface JmespathRuntime<T> extends Comparator<T> {

    RuntimeType typeOf(T value);

    default boolean is(T value, RuntimeType type) {
        return typeOf(value).equals(type);
    }

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
                Iterable<? extends T> iterable = toIterable(value);
                if (iterable instanceof Collection<?>) {
                    return !((Collection<?>) iterable).isEmpty();
                } else {
                    return iterable.iterator().hasNext();
                }
            default:
                throw new IllegalStateException();
        }
    }

    default boolean equal(T a, T b) {
        return Objects.equals(a, b);
    }

    default int compare(T a, T b) {
        // TODO: More types
        return EvaluationUtils.compareNumbersWithPromotion(asNumber(a), asNumber(b));
    }

    T createNull();

    T createBoolean(boolean b);

    boolean asBoolean(T value);

    T createString(String string);

    String asString(T value);

    T createNumber(Number value);

    NumberType numberType(T value);

    Number asNumber(T value);

    // Common collection operations

    Number length(T value);

    // Iterating over arrays or objects
    Iterable<? extends T> toIterable(T value);

    // Arrays

    T element(T array, T index);

    default T slice(T array, T startNumber, T stopNumber, T stepNumber) {
        // TODO: Move to a static method somewhere
        if (!is(array, RuntimeType.ARRAY)) {
            return createNull();
        }

        JmespathRuntime.ArrayBuilder<T> output = arrayBuilder();
        int length = length(array).intValue();

        int step = asNumber(stepNumber).intValue();
        if (step == 0) {
            throw new JmespathException(JmespathExceptionType.INVALID_VALUE, "invalid-value");
        }

        int start;
        if (is(startNumber, RuntimeType.NULL)) {
            start = step > 0 ? 0 : length - 1;
        } else {
            start = asNumber(startNumber).intValue();
            if (start < 0) {
                start = length + start;
            }
            if (start < 0) {
                start = 0;
            } else if (start > length - 1) {
                start = length - 1;
            }
        }

        int stop;
        if (is(stopNumber, RuntimeType.NULL)) {
            stop = step > 0 ? length : -1;
        } else {
            stop = asNumber(stopNumber).intValue();
            if (stop < 0) {
                stop = length + stop;
            }

            if (stop < 0) {
                stop = -1;
            } else if (stop > length) {
                stop = length;
            }
        }

        if (start < stop) {
            if (step > 0) {
                // TODO: Use iterate(...) when step == 1
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

    ArrayBuilder<T> arrayBuilder();

    interface ArrayBuilder<T> {

        void add(T value);

        void addAll(T array);

        T build();
    }

    // Objects

    T value(T value, T name);

    ObjectBuilder<T> objectBuilder();

    interface ObjectBuilder<T> {

        void put(T key, T value);

        void putAll(T object);

        T build();
    }

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
                for (T element : toIterable(value)) {
                    if (first) {
                        first = false;
                    } else {
                        arrayStringBuilder.append(", ");
                    }
                    arrayStringBuilder.append(toString(element));
                }
                arrayStringBuilder.append("]");
                return arrayStringBuilder.toString();
            case OBJECT:
                StringBuilder objectStringBuilder = new StringBuilder();
                objectStringBuilder.append("{");
                boolean firstKey = true;
                for (T key : toIterable(value)) {
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
}

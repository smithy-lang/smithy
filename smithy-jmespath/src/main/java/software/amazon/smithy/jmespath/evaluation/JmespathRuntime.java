/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.function.BiFunction;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.ResolvedFunctionExpression;

/**
 * An interface to provide the operations needed for JMESPath expression evaluation
 * based on any runtime representation of JSON values.
 * <p>
 * Several methods have default implementations that are at least correct,
 * but implementors can override them with more efficient implementations.
 * <p>
 * In the documentation of the required behavior of each method,
 * note that conditions like "if the value is NULL",
 * refer to T value where typeOf(value) returns RuntimeType.NULL.
 * A runtime may or may not use a Java `null` value for this purpose.
 */
public interface JmespathRuntime<T> extends Comparator<T> {

    // TODO: If true, you can't call the non-abstract versions blah blah
    default boolean isAbstract() {
        return false;
    }

    ///////////////////////////////
    // General Operations
    ///////////////////////////////

    /**
     * Returns the basic type of the given value: NULL, BOOLEAN, STRING, NUMBER, OBJECT, or ARRAY.
     * <p>
     * MUST NOT ever return EXPRESSION or ANY.
     */
    RuntimeType typeOf(T value);

    default T abstractTypeOf(T value) {
        return createString(typeOf(value).toString());
    }

    /**
     * Shorthand for {@code typeOf(value).equals(type)}.
     */
    default boolean is(T value, RuntimeType type) {
        return typeOf(value).equals(type);
    }

    default T abstractIs(T value, RuntimeType type) {
        return abstractEqual(abstractTypeOf(value), createString(type.toString()));
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

    default T abstractEqual(T a, T b) {
        return createBoolean(equal(a, b));
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

    default T abstractCompare(T a, T b) {
        return createNumber(compare(a, b));
    }

    default T createAny(RuntimeType runtimeType) {
        throw new UnsupportedOperationException("anyValue called on concrete runtime");
    }

    default T either(T left, T right) {
        throw new UnsupportedOperationException("either called on concrete runtime");
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
                arrayStringBuilder.append('[');
                boolean first = true;
                for (T element : asIterable(value)) {
                    if (first) {
                        first = false;
                    } else {
                        // The compliance tests actually require ',' rather than ", " :P
                        arrayStringBuilder.append(',');
                    }
                    arrayStringBuilder.append(toString(element));
                }
                arrayStringBuilder.append(']');
                return arrayStringBuilder.toString();
            case OBJECT:
                StringBuilder objectStringBuilder = new StringBuilder();
                objectStringBuilder.append('{');
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
                objectStringBuilder.append('}');
                return objectStringBuilder.toString();
            default:
                throw new IllegalStateException();
        }
    }

    default T abstractToString(T value) {
        return createString(toString(value));
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

    /**
     * Creates a BOOLEAN value.
     */
    T createBoolean(boolean b);

    /**
     * If the given value is a BOOLEAN, return it as a boolean.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    boolean asBoolean(T value);

    ///////////////////////////////
    // STRINGs
    ///////////////////////////////

    /**
     * Creates a STRING value.
     */
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

    /**
     * Creates a NUMBER value.
     */
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

    /**
     * Creates a new ArrayBuilder.
     */
    // TODO: Default implementation of wrapping an immutable array value and using append and concat?
    ArrayBuilder<T> arrayBuilder();

    /**
     * A builder interface for new ARRAY values.
     */
    interface ArrayBuilder<T> {

        /**
         * Adds the given value to the array being built.
         */
        ArrayBuilder<T> add(T value);

        /**
         * If the given value is an ARRAY, adds all the elements of the array.
         * If the given value is an OBJECT, adds all the keys of the object.
         * Otherwise, throws a JmespathException of type INVALID_TYPE.
         */
        ArrayBuilder<T> addAll(T collection);

        /**
         * Builds the new ARRAY value being built.
         */
        T build();
    }

    /**
     * If the given value is an ARRAY, returns the element at the given index.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    T element(T array, int index);

    /**
     * If the given value is an ARRAY, returns the specified slice.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     * <p>
     * Start and stop will always be non-negative, and step will always be non-zero.
     */
    default T slice(T array, int start, int stop, int step) {
        if (is(array, RuntimeType.NULL)) {
            return createNull();
        }

        JmespathRuntime.ArrayBuilder<T> output = arrayBuilder();

        if (start < stop) {
            // If step is negative, the result is an empty array.
            if (step > 0) {
                for (int idx = start; idx < stop; idx += step) {
                    output.add(element(array, idx));
                }
            }
        } else {
            // If step is positive, the result is an empty array.
            if (step < 0) {
                // List is iterating in reverse
                for (int idx = start; idx > stop; idx += step) {
                    output.add(element(array, idx));
                }
            }
        }
        return output.build();
    }

    ///////////////////////////////
    // OBJECTs
    ///////////////////////////////

    /**
     * Creates a new ObjectBuilder.
     */
    // TODO: Default implementation of wrapping an immutable object value and using merge?
    // Don't want any concrete runtime to use that though.
    ObjectBuilder<T> objectBuilder();

    /**
     * A builder interface for new OBJECT values.
     */
    interface ObjectBuilder<T> {

        /**
         * Adds the given key/value pair to the object being built.
         */
        void put(T key, T value);

        /**
         * If the given value is an OBJECT, adds all of its key/value pairs.
         * Otherwise, throws a JmespathException of type INVALID_TYPE.
         */
        void putAll(T object);

        /**
         * Builds the new OBJECT value being built.
         */
        T build();
    }

    /**
     * If the given value is an OBJECT, returns the value mapped to the given key.
     * Otherwise, returns NULL.
     */
    T value(T object, T key);

    ///////////////////////////////
    // Common collection operations for ARRAYs and OBJECTs
    ///////////////////////////////

    /**
     * Returns the number of elements in an ARRAY or the number of keys in an OBJECT.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    int length(T value);

    default T abstractLength(T value) {
        return createNumber(length(value));
    }

    /**
     * Iterate over the elements of an ARRAY or the keys of an OBJECT.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    Iterable<? extends T> asIterable(T value);

    ///////////////////////////////
    // Functions
    ///////////////////////////////

    /**
     * Resolve a function expression.
     * The runtime can provide more optimized implementations of specific functions,
     * or more abstracted versions for abstract runtimes.
     * It can also recognize runtime-native functions.
     *
     * @return
     */
    default Function<T> resolveFunction(String name) {
        return null;
    }
}

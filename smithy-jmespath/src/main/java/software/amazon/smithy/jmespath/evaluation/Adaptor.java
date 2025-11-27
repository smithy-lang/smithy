package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public interface Adaptor<T> extends Comparator<T> {
    RuntimeType typeOf(T value);

    default boolean is(T value, RuntimeType type) {
        return typeOf(value).equals(type);
    }

    default boolean isTruthy(T value) {
        switch (typeOf(value)) {
            case NULL:  return false;
            case BOOLEAN: return toBoolean(value);
            case STRING: return !toString(value).isEmpty();
            case NUMBER: return true;
            case ARRAY: return !getArrayIterator(value).iterator().hasNext();
            case OBJECT: return isTruthy(getKeys(value));
            default: throw new IllegalStateException();
        }
    }

    default boolean equal(T a, T b) {
        return Objects.equals(a, b);
    }

    default int compare(T a, T b) {
        return EvaluationUtils.compareNumbersWithPromotion(toNumber(a), toNumber(b));
    }

    T createNull();

    T createBoolean(boolean b);

    boolean toBoolean(T value);

    T createString(String string);

    String toString(T value);

    T createNumber(Number value);

    Number toNumber(T value);

    // Arrays

    T length(T value);

    // TODO: rename to element
    T getArrayElement(T array, T index);

    default T slice(T array, T startNumber, T stopNumber, T stepNumber) {
        Adaptor.ArrayBuilder<T> output = arrayBuilder();
        int length = toNumber(length(array)).intValue();
        int step = toNumber(stepNumber).intValue();
        int start = is(startNumber, RuntimeType.NULL) ? (step > 0 ? 0 : length) : toNumber(startNumber).intValue();
        if (start < 0) {
            start = length + start;
        }
        int stop = is(stopNumber, RuntimeType.NULL) ? (step > 0 ? length : 0) : toNumber(stopNumber).intValue();
        if (stop < 0) {
            stop = length + stop;
        }

        if (start < stop) {
            // TODO: Use iterate(...) when step == 1
            for (int idx = start; idx < stop; idx += step) {
                output.add(getArrayElement(array, createNumber(idx)));
            }
        } else {
            // List is iterating in reverse
            for (int idx = start; idx > stop; idx += step) {
                output.add(getArrayElement(array, createNumber(idx - 1)));
            }
        }
        return output.build();
    }

    // TODO: rename to iterate
    Iterable<T> getArrayIterator(T array);

    ArrayBuilder<T> arrayBuilder();

    interface ArrayBuilder<T> {
        void add(T value);
        void addAll(T array);
        T build();
    }

    // Objects

    // TODO: rename to keys
    T getKeys(T value);

    // TODO: rename to value
    T getValue(T value, T name);

    ObjectBuilder<T> objectBuilder();

    interface ObjectBuilder<T> {
        void put(T key, T value);
        void putAll(T object);
        T build();
    }

    // TODO: T parseJson(String)?
}

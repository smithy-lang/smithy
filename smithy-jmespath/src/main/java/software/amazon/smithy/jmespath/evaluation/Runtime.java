package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.Comparator;
import java.util.Objects;

public interface Runtime<T> extends Comparator<T> {

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
            case ARRAY: return !iterate(value).iterator().hasNext();
            case OBJECT: return isTruthy(keys(value));
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

    // TODO: Might be better as a Number
    T length(T value);

    T element(T array, T index);

    default T slice(T array, T startNumber, T stopNumber, T stepNumber) {
        // TODO: Move to a static method somewhere
        Runtime.ArrayBuilder<T> output = arrayBuilder();
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
                output.add(element(array, createNumber(idx)));
            }
        } else {
            // List is iterating in reverse
            for (int idx = start; idx > stop; idx += step) {
                output.add(element(array, createNumber(idx - 1)));
            }
        }
        return output.build();
    }

    Iterable<T> iterate(T array);

    ArrayBuilder<T> arrayBuilder();

    interface ArrayBuilder<T> {
        void add(T value);
        void addAll(T array);
        T build();
    }

    // Objects

    T keys(T value);

    T value(T value, T name);

    ObjectBuilder<T> objectBuilder();

    interface ObjectBuilder<T> {
        void put(T key, T value);
        void putAll(T object);
        T build();
    }

    // TODO: T parseJson(String)?
    // Only worth it if we make parsing use the runtime as well,
    // and recognize LiteralExpressions that are wrapping a T somehow.
}

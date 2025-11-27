package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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
            case ARRAY: return !toList(value).isEmpty();
            case OBJECT: return !getPropertyNames(value).isEmpty();
            default: throw new IllegalStateException();
        }
    }

    T createNull();

    T createBoolean(boolean b);

    boolean toBoolean(T value);

    T createString(String string);

    String toString(T value);

    T createNumber(Number value);

    Number toNumber(T value);

    // Arrays

    // TODO: Or expose length() and at(int) primitives. Safe to assume random access,
    // but more annoying to not use enhanced for loops.
    // Have to double check consistent behavior around operations on non-lists
    List<T> toList(T value);

    ArrayBuilder<T> arrayBuilder();

    interface ArrayBuilder<T> {
        void add(T value);
        void addAll(T array);
        T build();
    }

    // Objects

    T getProperty(T value, T name);

    Collection<? extends T> getPropertyNames(T value);

    ObjectBuilder<T> objectBuilder();

    interface ObjectBuilder<T> {
        void put(T key, T value);
        void putAll(T object);
        T build();
    }

    // TODO: T parseJson(String)?

    // TODO: Move somewhere better and make this a default implementation of Adaptor.compare
    default int compare(T a, T b) {
        return EvaluationUtils.compareNumbersWithPromotion(toNumber(a), toNumber(b));
    }


}

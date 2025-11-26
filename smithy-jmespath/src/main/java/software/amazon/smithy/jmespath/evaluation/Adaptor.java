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

    boolean isTruthy(T value);

    T createNull();
    T createBoolean(boolean b);
    T createString(String string);
    T createNumber(Number value);

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

    Collection<T> getPropertyNames(T value);

    ObjectBuilder<T> objectBuilder();

    interface ObjectBuilder<T> {
        void put(T key, T value);
        T build();
    }

    // TODO: T parseJson(String)?
}

package software.amazon.smithy.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * TODO: Add JavaDoc.
 */
public final class SetUtils {
    private SetUtils() {}

    public static <T> Set<T> copyOf(Collection<? extends T> values) {
        return values.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(values));
    }

    public static <T> Set<T> of() {
        return Collections.emptySet();
    }

    public static <T> Set<T> of(T value) {
        return Collections.singleton(value);
    }

    public static <T> Set<T> of(T v1, T v2) {
        Set<T> result = new HashSet<>(2);
        result.add(v1);
        result.add(v2);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> of(T v1, T v2, T v3) {
        Set<T> result = new HashSet<>(3);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> of(T v1, T v2, T v3, T v4) {
        Set<T> result = new HashSet<>(4);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> of(T v1, T v2, T v3, T v4, T v5) {
        Set<T> result = new HashSet<>(5);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> of(T v1, T v2, T v3, T v4, T v5, T v6) {
        Set<T> result = new HashSet<>(6);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        result.add(v6);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> of(T v1, T v2, T v3, T v4, T v5, T v6, T v7) {
        Set<T> result = new HashSet<>(7);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        result.add(v6);
        result.add(v7);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> of(T v1, T v2, T v3, T v4, T v5, T v6, T v7, T v8) {
        Set<T> result = new HashSet<>(8);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        result.add(v6);
        result.add(v7);
        result.add(v8);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> of(T v1, T v2, T v3, T v4, T v5, T v6, T v7, T v8, T v9) {
        Set<T> result = new HashSet<>(9);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        result.add(v6);
        result.add(v7);
        result.add(v8);
        result.add(v9);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> of(T v1, T v2, T v3, T v4, T v5, T v6, T v7, T v8, T v9, T v10) {
        Set<T> result = new HashSet<>(10);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        result.add(v6);
        result.add(v7);
        result.add(v8);
        result.add(v9);
        result.add(v10);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> of(T... values) {
        HashSet<T> result = new HashSet<>(values.length);
        Collections.addAll(result, values);
        return Collections.unmodifiableSet(result);
    }

    /**
     * Creates a collector that collects into an unmodifiable set.
     *
     * <p>This is a polyfill equivalent of Java 10's
     * {@code Collectors#toUnmodifiableSet}.
     *
     * @param <T> Type of value to expect.
     */
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet);
    }
}

package software.amazon.smithy.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Immutable List utilities to polyfill Java 9+ features.
 */
public final class ListUtils {
    private ListUtils() {}

    public static <T> List<T> copyOf(Collection<? extends T> values) {
        return values.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public static <T> List<T> of() {
        return Collections.emptyList();
    }

    public static <T> List<T> of(T value) {
        return Collections.singletonList(value);
    }

    public static <T> List<T> of(T v1, T v2) {
        List<T> result = new ArrayList<>(2);
        result.add(v1);
        result.add(v2);
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> of(T v1, T v2, T v3) {
        List<T> result = new ArrayList<>(3);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> of(T v1, T v2, T v3, T v4) {
        List<T> result = new ArrayList<>(4);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> of(T v1, T v2, T v3, T v4, T v5) {
        List<T> result = new ArrayList<>(5);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> of(T v1, T v2, T v3, T v4, T v5, T v6) {
        List<T> result = new ArrayList<>(6);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        result.add(v6);
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> of(T v1, T v2, T v3, T v4, T v5, T v6, T v7) {
        List<T> result = new ArrayList<>(7);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        result.add(v6);
        result.add(v7);
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> of(T v1, T v2, T v3, T v4, T v5, T v6, T v7, T v8) {
        List<T> result = new ArrayList<>(8);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        result.add(v6);
        result.add(v7);
        result.add(v8);
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> of(T v1, T v2, T v3, T v4, T v5, T v6, T v7, T v8, T v9) {
        List<T> result = new ArrayList<>(9);
        result.add(v1);
        result.add(v2);
        result.add(v3);
        result.add(v4);
        result.add(v5);
        result.add(v6);
        result.add(v7);
        result.add(v8);
        result.add(v9);
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> of(T v1, T v2, T v3, T v4, T v5, T v6, T v7, T v8, T v9, T v10) {
        List<T> result = new ArrayList<>(10);
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
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> of(T... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    /**
     * Creates a collector that collects into an unmodifiable list.
     *
     * <p>This is a polyfill equivalent of Java 10's
     * {@code Collectors#toUnmodifiableList}.
     *
     * @param <T> Type of value to expect.
     */
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
    }
}

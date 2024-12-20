/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.AbstractList;
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

    /**
     * Creates an immutable copy of the given list.
     *
     * @param values The collection to make an immutable list of.
     * @param <T> the List's value type.
     * @return An immutable List copy.
     */
    public static <T> List<T> copyOf(Collection<? extends T> values) {
        return values.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(values));
    }

    /**
     * Returns an unmodifiable list containing zero entries.
     *
     * @param <T> the List's value type.
     * @return an empty List.
     */
    public static <T> List<T> of() {
        return Collections.emptyList();
    }

    /**
     * Returns an unmodifiable list containing a single entry.
     *
     * @param value the List's value.
     * @param <T> the List's value type.
     * @return a List containing the specified value.
     * @throws NullPointerException if the value is {@code null}.
     */
    public static <T> List<T> of(T value) {
        return Collections.singletonList(value);
    }

    /**
     * Returns an unmodifiable list containing two entries.
     *
     * @param value1 The first value.
     * @param value2 The second value.
     * @param <T> the List's value type.
     * @return a List containing the specified values.
     */
    @SuppressWarnings("varargs")
    public static <T> List<T> of(T value1, T value2) {
        // Note that AbstractList is immutable by default.
        return new AbstractList<T>() {
            @Override
            public T get(int index) {
                switch (index) {
                    case 0:
                        return value1;
                    case 1:
                        return value2;
                    default:
                        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 2");
                }
            }

            @Override
            public int size() {
                return 2;
            }
        };
    }

    /**
     * Returns an unmodifiable list containing any number of entries.
     *
     * @param values the List's values.
     * @param <T> the List's value type.
     * @return a List containing the specified values.
     * @throws NullPointerException if any value is {@code null}.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
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
     * @return a Collector that accumulates the entries into an unmodifiable List.
     */
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Immutable Map utilities to polyfill Java 9+ features.
 */
public final class MapUtils {
    private MapUtils() {}

    /**
     * Creates an immutable copy of the given map.
     *
     * @param map The map to make an immutable copy of
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @return An immutable Map copy
     */
    public static <K, V> Map<K, V> copyOf(Map<? extends K, ? extends V> map) {
        return map.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(map));
    }

    /**
     * Creates an ordered immutable copy of the given map.
     *
     * @param map The map to make an immutable copy of
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @return An ordered immutable Map copy that maintains the order of the original.
     */
    public static <K, V> Map<K, V> orderedCopyOf(Map<? extends K, ? extends V> map) {
        return map.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    /**
     * Returns an unmodifiable map containing zero mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @return an empty Map
     */
    public static <K, V> Map<K, V> of() {
        return Collections.emptyMap();
    }

    /**
     * Returns an unmodifiable map containing a single mapping.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the mapping's key
     * @param v1 the mapping's value
     * @return a Map containing the specified mapping
     * @throws NullPointerException if the key or the value is {@code null}
     */
    public static <K, V> Map<K, V> of(K k1, V v1) {
        return Collections.singletonMap(k1, v1);
    }

    /**
     * Returns an unmodifiable map containing two mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the first mapping's key
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key
     * @param v2 the second mapping's value
     * @return a Map containing the specified mappings
     * @throws IllegalArgumentException if the keys are duplicates
     * @throws NullPointerException if any key or value is {@code null}
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> result = new HashMap<>(2);
        result.put(k1, v1);
        result.put(k2, v2);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an unmodifiable map containing three mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the first mapping's key
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key
     * @param v3 the third mapping's value
     * @return a Map containing the specified mappings
     * @throws IllegalArgumentException if there are any duplicate keys
     * @throws NullPointerException if any key or value is {@code null}
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> result = new HashMap<>(3);
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an unmodifiable map containing four mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the first mapping's key
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key
     * @param v4 the fourth mapping's value
     * @return a Map containing the specified mappings
     * @throws IllegalArgumentException if there are any duplicate keys
     * @throws NullPointerException if any key or value is {@code null}
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Map<K, V> result = new HashMap<>(4);
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an unmodifiable map containing five mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the first mapping's key
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key
     * @param v5 the fifth mapping's value
     * @return a Map containing the specified mappings
     * @throws IllegalArgumentException if there are any duplicate keys
     * @throws NullPointerException if any key or value is {@code null}
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Map<K, V> result = new HashMap<>(5);
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        result.put(k5, v5);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an unmodifiable map containing six mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the first mapping's key
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key
     * @param v6 the sixth mapping's value
     * @return a Map containing the specified mappings
     * @throws IllegalArgumentException if there are any duplicate keys
     * @throws NullPointerException if any key or value is {@code null}
     */
    public static <K, V> Map<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6
    ) {
        Map<K, V> result = new HashMap<>(6);
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        result.put(k5, v5);
        result.put(k6, v6);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an unmodifiable map containing seven mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the first mapping's key
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key
     * @param v6 the sixth mapping's value
     * @param k7 the seventh mapping's key
     * @param v7 the seventh mapping's value
     * @return a Map containing the specified mappings
     * @throws IllegalArgumentException if there are any duplicate keys
     * @throws NullPointerException if any key or value is {@code null}
     */
    public static <K, V> Map<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6,
            K k7,
            V v7
    ) {
        Map<K, V> result = new HashMap<>(7);
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        result.put(k5, v5);
        result.put(k6, v6);
        result.put(k7, v7);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an unmodifiable map containing eight mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the first mapping's key
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key
     * @param v6 the sixth mapping's value
     * @param k7 the seventh mapping's key
     * @param v7 the seventh mapping's value
     * @param k8 the eighth mapping's key
     * @param v8 the eighth mapping's value
     * @return a Map containing the specified mappings
     * @throws IllegalArgumentException if there are any duplicate keys
     * @throws NullPointerException if any key or value is {@code null}
     */
    public static <K, V> Map<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6,
            K k7,
            V v7,
            K k8,
            V v8
    ) {
        Map<K, V> result = new HashMap<>(8);
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        result.put(k5, v5);
        result.put(k6, v6);
        result.put(k7, v7);
        result.put(k8, v8);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an unmodifiable map containing nine mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the first mapping's key
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key
     * @param v6 the sixth mapping's value
     * @param k7 the seventh mapping's key
     * @param v7 the seventh mapping's value
     * @param k8 the eighth mapping's key
     * @param v8 the eighth mapping's value
     * @param k9 the ninth mapping's key
     * @param v9 the ninth mapping's value
     * @return a Map containing the specified mappings
     * @throws IllegalArgumentException if there are any duplicate keys
     * @throws NullPointerException if any key or value is {@code null}
     */
    public static <K, V> Map<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6,
            K k7,
            V v7,
            K k8,
            V v8,
            K k9,
            V v9
    ) {
        Map<K, V> result = new HashMap<>(9);
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        result.put(k5, v5);
        result.put(k6, v6);
        result.put(k7, v7);
        result.put(k8, v8);
        result.put(k9, v9);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an unmodifiable map containing ten mappings.
     *
     * @param <K> the Map's key type
     * @param <V> the Map's value type
     * @param k1 the first mapping's key
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key
     * @param v6 the sixth mapping's value
     * @param k7 the seventh mapping's key
     * @param v7 the seventh mapping's value
     * @param k8 the eighth mapping's key
     * @param v8 the eighth mapping's value
     * @param k9 the ninth mapping's key
     * @param v9 the ninth mapping's value
     * @param k10 the tenth mapping's key
     * @param v10 the tenth mapping's value
     * @return a Map containing the specified mappings
     * @throws IllegalArgumentException if there are any duplicate keys
     * @throws NullPointerException if any key or value is {@code null}
     */
    public static <K, V> Map<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6,
            K k7,
            V v7,
            K k8,
            V v8,
            K k9,
            V v9,
            K k10,
            V v10
    ) {
        Map<K, V> result = new HashMap<>(10);
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        result.put(k5, v5);
        result.put(k6, v6);
        result.put(k7, v7);
        result.put(k8, v8);
        result.put(k9, v9);
        result.put(k10, v10);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Creates an immutable {@link Map.Entry} from the given key and value.
     *
     * @param key Key to set in the {@code Map.Entry}.
     * @param value Value to set in the {@code Map.Entry}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return Returns the created entry.
     * @throws NullPointerException if the key or value are null.
     */
    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return Pair.of(Objects.requireNonNull(key), Objects.requireNonNull(value));
    }

    /**
     * Creates an unmodifiable Map from an array of {@code Map.Entry} values.
     *
     * @param entries Map entries to add to the created {@code Map}.
     * @param <K> Map key type.
     * @param <V> Map value type.
     * @return Returns an unmodifiable map of the given entries.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <K, V> Map<K, V> ofEntries(Map.Entry<? extends K, ? extends V>... entries) {
        if (entries.length == 0) {
            return MapUtils.of();
        } else if (entries.length == 1) {
            return MapUtils.of(entries[0].getKey(), entries[0].getValue());
        } else {
            Map<K, V> result = new HashMap<>(entries.length);
            for (Map.Entry<? extends K, ? extends V> entry : entries) {
                result.put(entry.getKey(), entry.getValue());
            }
            return Collections.unmodifiableMap(result);
        }
    }

    /**
     * Creates a collector that collects into an unmodifiable Map.
     *
     * <p>This is a polyfill equivalent of Java 10's
     * {@code Collectors#toUnmodifiableMap}.
     *
     * @param <T> the type to retrieve keys and values from.
     * @param <K> the Map's key type.
     * @param <U> the Map's value type.
     * @param keyMapper Function that retrieves the key.
     * @param valueMapper Function that retrieves the value.
     * @return a Collector that accumulates the entries into an unmodifiable Map.
     */
    @SuppressWarnings("unchecked")
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper
    ) {
        return Collectors.collectingAndThen(
                Collectors.toMap(
                        Objects.requireNonNull(keyMapper, "keyMapper"),
                        Objects.requireNonNull(valueMapper, "valueMapper")),
                Collections::unmodifiableMap);
    }
}

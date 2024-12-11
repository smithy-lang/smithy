/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import software.amazon.smithy.utils.Pair;

/**
 * Caches instances of type V, associating them with a given key and Type.
 *
 * <p>Cached values are invalidated if the requested type is not referentially
 * equal to the cached type. This ensures that the most recently loaded versions
 * of classes are being used.
 *
 * @param <K> The type of the cache key.
 * @param <V> The type of the cached value.
 */
final class IdentityClassCache<K, V> {

    private final ConcurrentMap<K, Pair<Type, V>> cache = new ConcurrentHashMap<>();

    /**
     * Retrieves cached value for the key associated with the given Type,
     * computing the value to store if the key isn't present, or if the cache is
     * invalidated.
     *
     * @param forKey The key of the cached value to retrieve.
     * @param forClass The Type that should be associated with the cached value.
     * @param supplierIfNotPresent Function to compute cached value.
     * @return Cached value.
     */
    V getForClass(K forKey, Type forClass, Supplier<? extends V> supplierIfNotPresent) {
        return cache.compute(forKey, (key, current) -> {
            if (current == null || current.getLeft() != forClass) {
                // The key was not present, or the cache was invalidated.
                return Pair.of(forClass, supplierIfNotPresent.get());
            } else {
                return current;
            }
        }).getRight();
    }
}

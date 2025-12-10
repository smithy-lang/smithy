/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A default implementation of {@link JmespathRuntime.ObjectBuilder}.
 * using a {@link Map} as the backing store.
 */
public class MapObjectBuilder<T> implements JmespathRuntime.ObjectBuilder<T> {

    private final JmespathRuntime<T> runtime;
    private final Map<String, T> result = new HashMap<>();
    private final Function<Map<String, T>, T> wrapping;

    public MapObjectBuilder(JmespathRuntime<T> runtime, Function<Map<String, T>, T> wrapping) {
        this.runtime = runtime;
        this.wrapping = wrapping;
    }

    @Override
    public void put(T key, T value) {
        result.put(runtime.asString(key), value);
    }

    @Override
    public void putAll(T object) {
        // A fastpath for when object is a Map doesn't quite work,
        // because you would need to know that it's specifically a Map<String, T>.
        for (T key : runtime.asIterable(object)) {
            result.put(runtime.asString(key), runtime.value(object, key));
        }
    }

    @Override
    public T build() {
        return wrapping.apply(result);
    }
}

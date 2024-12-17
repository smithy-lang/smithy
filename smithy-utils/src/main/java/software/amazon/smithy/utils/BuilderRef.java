/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the creation, copying, and reuse of values created by builders.
 *
 * <p>BuilderRef manages the state of a mutable value contained in a builder.
 * It ensures that things like arrays built up in a builder can be used
 * directly in objects being built without copies, and it allows the builder
 * to be reused without mutating previously built objects.
 *
 * <p>Smithy uses lots of builders to build up immutable objects. To make an
 * immutable object in Java, you need to make defensive copies of things like
 * lists, sets, and maps, but doing that for every single one of these types
 * in builders results in lots of copies. One option is to just use the lists
 * et al contained in builders directly in the built objects, but that runs the
 * risk of the builder being further mutated and thus mutating the object.
 * Another option is to clear out the state of the builder and give "ownership"
 * of lists et al to built objects, but that means builders can't easily be
 * used to build up multiple objects (and while that rare, it's a use case that
 * probably *should* work, and always has worked in Smithy).
 *
 * <p>BuilderRef creates the value if needed when the builder needs to mutate
 * the wrapped value (see {@link #get}. It creates an immutable copy of the value
 * when {@link #copy} is called, and an immutable "borrowed" reference to the
 * copied value is maintained in the reference. This borrowed copy can be queried
 * using {@link #peek}, but it can't be mutated. If the reference only has a
 * borrowed value but attempts to call {@link #get}, then a copy of the borrowed
 * value is created and returned from {@code get()}.
 *
 * @param <T> Type of value being created.
 */
public interface BuilderRef<T> extends CopyOnWriteRef<T> {

    /**
     * Creates an immutable copy of {@code T}.
     *
     * <p>Subsequent calls to {@link #hasValue()} may or may not return true
     * after this method is called.
     *
     * @return Returns the copied immutable value.
     */
    T copy();

    /**
     * Checks if the reference currently has a borrowed or owned value.
     *
     * <p>This method does not check if the contained value is considered
     * empty. This method only returns true if the reference contains any
     * kind of previously built value. This might be useful for builder
     * methods that attempt to remove values from the contained object.
     * If there is no contained object, then there's no need to create
     * one just to remove a value from an empty container.
     *
     * @return Returns true if the reference contains a value of any kind.
     */
    boolean hasValue();

    /**
     * Removes any borrowed or owned values from the reference.
     *
     * <p>Subsequent calls to {@link #hasValue()} will return false after
     * this method is called.
     */
    void clear();

    /**
     * Creates a builder reference to an unordered map.
     *
     * @param <K> Type of key of the map.
     * @param <V> Type of value of the map.
     * @return Returns the created map.
     */
    static <K, V> BuilderRef<Map<K, V>> forUnorderedMap() {
        return new DefaultBuilderRef<>(HashMap::new,
                HashMap::new,
                Collections::unmodifiableMap,
                Collections::emptyMap);
    }

    /**
     * Creates a builder reference to a ordered map.
     *
     * @param <K> Type of key of the map.
     * @param <V> Type of value of the map.
     * @return Returns the created map.
     */
    static <K, V> BuilderRef<Map<K, V>> forOrderedMap() {
        return new DefaultBuilderRef<>(LinkedHashMap::new,
                LinkedHashMap::new,
                Collections::unmodifiableMap,
                Collections::emptyMap);
    }

    /**
     * Creates a builder reference to a list.
     *
     * @param <T> Type of value in the list.
     * @return Returns the created list.
     */
    static <T> BuilderRef<List<T>> forList() {
        return new DefaultBuilderRef<>(ArrayList::new,
                ArrayList::new,
                Collections::unmodifiableList,
                Collections::emptyList);
    }

    /**
     * Creates a builder reference to an unordered set.
     *
     * @param <T> Type of value in the set.
     * @return Returns the created set.
     */
    static <T> BuilderRef<Set<T>> forUnorderedSet() {
        return new DefaultBuilderRef<>(HashSet::new,
                HashSet::new,
                Collections::unmodifiableSet,
                Collections::emptySet);
    }

    /**
     * Creates a builder reference to an ordered set.
     *
     * @param <T> Type of value in the set.
     * @return Returns the created set.
     */
    static <T> BuilderRef<Set<T>> forOrderedSet() {
        return new DefaultBuilderRef<>(LinkedHashSet::new,
                LinkedHashSet::new,
                Collections::unmodifiableSet,
                Collections::emptySet);
    }
}

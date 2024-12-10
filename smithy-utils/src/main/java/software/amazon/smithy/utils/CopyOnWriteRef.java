/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.function.Function;

/**
 * Provides a way to reference a {@code T} value, and copy the value if
 * needed in order to mutate a copy.
 *
 * <p>"peeking" the value using {@link  #peek()} will return the referenced
 * value and will not create a copy. "getting" the value using {@link #get()}
 * will create a copy of the peeked value if one was not already created.
 *
 * @param <T> Type of value to reference.
 */
interface CopyOnWriteRef<T> {

    /**
     * Gets a mutable {@code T} from the reference, creating one if needed.
     *
     * @return Returns a mutable {@code T} value.
     */
    T get();

    /**
     * Gets an immutable {@code T} from the reference, reusing borrowed
     * values if possible, and creating owned values if needed.
     *
     * <p>Attempting to mutate the provided value <em>should</em> fail
     * at runtime, but even if it doesn't, doing so could inadvertently
     * mutate previously built objects.
     *
     * @return Returns an immutable peeked {@code T} value.
     */
    T peek();

    /**
     * Creates a reference to a value that is borrowed, and a copy needs to be
     * created in order to get a mutable reference.
     *
     * @param borrowedValue The value being referenced.
     * @param copyFunction The function used to copy the value when a mutable
     *                     reference is requested via {@link #get()}.
     * @param <T> The type of value being referenced.
     * @return Returns the reference.
     */
    static <T> CopyOnWriteRef<T> fromBorrowed(T borrowedValue, Function<T, T> copyFunction) {
        return new CopyOnWriteRef<T>() {
            private T copy;

            @Override
            public T peek() {
                return copy != null ? copy : borrowedValue;
            }

            @Override
            public T get() {
                T result = copy;
                if (result == null) {
                    copy = result = copyFunction.apply(borrowedValue);
                }
                return result;
            }
        };
    }

    /**
     * Creates a reference to a value that is mutable and does not need to be
     * copied when {@link #get()} is called.
     *
     * @param owned Value to reference.
     * @param <T> Type of value being referenced.
     * @return Returns the reference.
     */
    static <T> CopyOnWriteRef<T> fromOwned(T owned) {
        return new CopyOnWriteRef<T>() {
            @Override
            public T peek() {
                return owned;
            }

            @Override
            public T get() {
                return owned;
            }
        };
    }
}

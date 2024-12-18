/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Functions that make working with optionals easier.
 */
public final class OptionalUtils {
    /**
     * This class should be used statically.
     */
    private OptionalUtils() {}

    /**
     * Retrieves the value of the optional if present or invokes the supplier
     * for a value.
     *
     * @param value Value to check.
     * @param supplier Supplier for a value if no value is present.
     * @param <T> Type of value.
     * @return A value of type T.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> or(Optional<T> value, Supplier<Optional<? extends T>> supplier) {
        return value.isPresent() ? value : (Optional<T>) supplier.get();
    }

    /**
     * Converts an Optional into a Stream that can be used in a flatmap.
     *
     * <p>This is a polyfill of Java 9's {@code Optional#stream}.
     *
     * @param value Value to convert to a stream.
     * @param <T> Value type.
     * @return A stream that contains a present value or a stream that is empty.
     */
    public static <T> Stream<T> stream(Optional<T> value) {
        return value.map(Stream::of).orElseGet(Stream::empty);
    }

    /**
     * Invokes a consumer if the Optional has a value, otherwise invoked a
     * Runnable when the Optional is empty.
     *
     * @param value Value to check.
     * @param action Action to invoke if a value is present.
     * @param emptyAction Runnable to invoke if a value is not present.
     * @param <T> Type of value.
     */
    public static <T> void ifPresentOrElse(Optional<T> value, Consumer<T> action, Runnable emptyAction) {
        if (value.isPresent()) {
            action.accept(value.get());
        } else {
            emptyAction.run();
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Default implementation of {@link BuilderRef}.
 *
 * <p>This class is not threadsafe; it should only be used to build up
 * other objects using {@link #copy()}.
 *
 * @param <T> The type of value being built, borrowed, and copied.
 */
final class DefaultBuilderRef<T> implements BuilderRef<T> {

    private final Supplier<T> ctor;
    private final Function<T, T> copyCtor;
    private final Function<T, T> immutableWrapper;
    private final Supplier<T> emptyCtorOptimization;
    private T immutableOwned;
    private T owned;
    private T borrowed;

    DefaultBuilderRef(
            Supplier<T> ctor,
            Function<T, T> copyCtor,
            Function<T, T> immutableWrapper,
            Supplier<T> emptyCtorOptimization
    ) {
        this.ctor = ctor;
        this.copyCtor = copyCtor;
        this.immutableWrapper = immutableWrapper;
        this.emptyCtorOptimization = emptyCtorOptimization;
    }

    @Override
    public T get() {
        if (owned != null) {
            return owned;
        } else if (borrowed != null) {
            // The reference has a borrowed value, so it can't be mutated. Copy it and make it owned.
            return setOwned(applyCopyCtor(borrowed));
        } else {
            // The reference has no owned or borrowed value, so create a new owned value.
            return setOwned(applyCtor());
        }
    }

    @Override
    public T peek() {
        if (immutableOwned != null) {
            return immutableOwned;
        } else if (owned != null) {
            // The reference has not yet created an immutable wrapper of the owned value.
            immutableOwned = applyImmutableWrapper(owned);
            return immutableOwned;
        } else if (borrowed != null) {
            // The reference already contains an immutable borrowed value.
            return borrowed;
        } else if (emptyCtorOptimization != null) {
            return applyEmptyCtorOptimization();
        } else {
            // Create and store an owned value, then create an immutable borrowed reference recursively.
            setOwned(applyCtor());
            return peek();
        }
    }

    @Override
    public T copy() {
        if (owned != null) {
            // The currently owned value will be the copied result. The reference will keep a "borrowed"
            // pointer to this value. Use peek() to get or create an immutably wrapped owned value.
            T result = peek();
            setOwned(null);
            borrowed = result;
            return result;
        } else if (borrowed != null) {
            // Copy the borrowed value and make it immutable.
            return applyImmutableWrapper(applyCopyCtor(borrowed));
        } else if (emptyCtorOptimization != null) {
            // The value is empty and was never created, so call the empty ctor supplier if available.
            return applyEmptyCtorOptimization();
        } else {
            // Cache the value so subsequent calls to peek reuse the computed value.
            borrowed = applyImmutableWrapper(applyCtor());
            return borrowed;
        }
    }

    @Override
    public boolean hasValue() {
        return owned != null || borrowed != null;
    }

    @Override
    public void clear() {
        setOwned(null);
    }

    private T setOwned(T value) {
        owned = value;
        immutableOwned = null;
        borrowed = null;
        return value;
    }

    private T applyCtor() {
        return Objects.requireNonNull(ctor.get(), "BuilderRef 'ctor' must not return null");
    }

    private T applyCopyCtor(T value) {
        return Objects.requireNonNull(copyCtor.apply(value), "BuilderRef 'copyCtor' must not return null");
    }

    private T applyImmutableWrapper(T value) {
        return Objects.requireNonNull(immutableWrapper.apply(value),
                "BuilderRef 'immutableWrapper' must not return null");
    }

    private T applyEmptyCtorOptimization() {
        return Objects.requireNonNull(emptyCtorOptimization.get(),
                "BuilderRef 'emptyCtorOptimization' must not return null");
    }
}

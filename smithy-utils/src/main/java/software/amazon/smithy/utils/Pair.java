/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Generic immutable pair of values.
 *
 * @param <L> Left value type.
 * @param <R> Right value type.
 */
public final class Pair<L, R> implements Map.Entry<L, R> {
    public final L left;
    public final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Creates a Pair from the given values.
     *
     * @param left Left value.
     * @param right Right value.
     * @param <L> Left value type.
     * @param <R> Right value type.
     * @return Returns the created Pair.
     */
    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    /**
     * Creates a {@link Stream} of zero or one Pairs if the mapping function
     * returns a non-empty {@link Optional}.
     *
     * @param left Value to set as the left side of the pair.
     * @param f Mapping function that accepts the left value and returns an
     *  Optional value.
     * @param <L> Left value type.
     * @param <R> Right value type.
     * @return Returns a Stream that contains either a single Pair if the
     *  Optional is not empty, or an empty Stream.
     */
    public static <L, R> Stream<Pair<L, R>> flatMapStream(L left, Function<L, Optional<R>> f) {
        return OptionalUtils.stream(f.apply(left).map(right -> Pair.of(left, right)));
    }

    public static <L, R> Stream<Pair<L, R>> flatMapStream(L left, Supplier<Optional<R>> f) {
        return OptionalUtils.stream(f.get().map(right -> Pair.of(left, right)));
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    @Override
    public L getKey() {
        return getLeft();
    }

    @Override
    public R getValue() {
        return getRight();
    }

    @Override
    public R setValue(R value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "(" + left + ", " + right + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }

        Pair other = (Pair) o;
        return left.equals(other.left) && right.equals(other.right);
    }

    @Override
    public int hashCode() {
        return left.hashCode() + right.hashCode();
    }
}

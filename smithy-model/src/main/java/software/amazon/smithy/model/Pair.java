/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model;

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
public final class Pair<L, R> {

    private final L left;
    private final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
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
        return f.apply(left).map(right -> new Pair<>(left, right)).stream();
    }

    public static <L, R> Stream<Pair<L, R>> flatMapStream(L left, Supplier<Optional<R>> f) {
        return f.get().map(right -> new Pair<>(left, right)).stream();
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
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

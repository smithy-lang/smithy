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

package software.amazon.smithy.utils;

import java.util.Objects;

/**
 * Generic immutable triple of values.
 *
 * @param <L> Left value type.
 * @param <M> Middle value type.
 * @param <R> Right value type.
 */
public final class Triple<L, M, R> {
    public final L left;
    public final M middle;
    public final R right;

    private Triple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    /**
     * Creates a Triple from the given values.
     *
     * @param left Left value.
     * @param middle Middle value.
     * @param right Right value.
     * @param <L> Left value type.
     * @param <M> Middle value type.
     * @param <R> Right value type.
     * @return Returns the created Triple.
     */
    public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right) {
        return new Triple<>(left, middle, right);
    }

    /**
     * Creates a Triple using a Pair and an additional value.
     *
     * @param pair Pair to use in the created triple.
     * @param right The additional value to use in the triple as "R".
     * @param <L> The left value type of the Pair.
     * @param <M> The right value type of the Pair.
     * @param <R> Additional value to use as the right part of the Triple.
     * @return Returns the created Triple.
     */
    public static <L, M, R> Triple<L, M, R> fromPair(Pair<L, M> pair, R right) {
        return of(pair.left, pair.right, right);
    }

    public L getLeft() {
        return left;
    }

    public M getMiddle() {
        return middle;
    }

    public R getRight() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Triple)) {
            return false;
        }

        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return Objects.equals(left, triple.left)
               && Objects.equals(middle, triple.middle)
               && Objects.equals(right, triple.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, middle, right);
    }

    @Override
    public String toString() {
        return "(" + left + ", " + middle + ", " + right + ")";
    }
}

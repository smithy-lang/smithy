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
 * @param <A> A value type.
 * @param <B> B value type.
 * @param <C> C value type.
 */
public final class Triple<A, B, C> {
    public final A a;
    public final B b;
    public final C c;

    private Triple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /**
     * Creates a Triple from the given values.
     *
     * @param a A value.
     * @param b B value.
     * @param c C value.
     * @param <A> A value type.
     * @param <B> B value type.
     * @param <C> C value type.
     * @return Returns the created Triple.
     */
    public static <A, B, C> Triple<A, B, C> of(A a, B b, C c) {
        return new Triple<>(a, b, c);
    }

    /**
     * Creates a Triple using a Pair and an additional value.
     *
     * @param pair Pair to use in the created triple.
     * @param c The additional value to use in the triple as "C".
     * @param <A> The left value type of the Pair.
     * @param <B> The right value type of the Pair.
     * @param <C> The additional value type.
     * @return Returns the created Triple.
     */
    public static <A, B, C> Triple<A, B, C> fromPair(Pair<A, B> pair, C c) {
        return of(pair.left, pair.right, c);
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public C getC() {
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Triple)) {
            return false;
        }

        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return Objects.equals(a, triple.a)
               && Objects.equals(b, triple.b)
               && Objects.equals(c, triple.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c);
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ", " + c + ")";
    }
}

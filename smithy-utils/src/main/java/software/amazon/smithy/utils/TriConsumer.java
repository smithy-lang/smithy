/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.function.BiConsumer;

/**
 * Similar to {@link BiConsumer}, but accepts three arguments.
 *
 * @param <T> The first argument type.
 * @param <U> The second argument type.
 * @param <V> The third argument type.
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {
    /**
     * Performs the operation on the given inputs.
     *
     * @param t is the first argument
     * @param u is the second argument
     * @param v is the third argument
     */
    void accept(T t, U u, V v);

    /**
     * Returns a composed {@link TriConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@link TriConsumer}
     * @throws NullPointerException if {@code after} is null
     */
    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);

        return (x, y, z) -> {
            accept(x, y, z);
            after.accept(x, y, z);
        };
    }
}

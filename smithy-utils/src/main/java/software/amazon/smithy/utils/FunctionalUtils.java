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

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utilities for working with functions, predicates, etc.
 */
public final class FunctionalUtils {

    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_TRUE = x -> true;

    private static final Function<Object, Object> IDENTITY = value -> value;

    private FunctionalUtils() {}

    /**
     * Negates a {@link Predicate}.
     *
     * @param predicate Predicate to negate.
     * @param <T> Value type of the predicate.
     * @return Returns a predicate that negates the given predicate.
     */
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

    /**
     * Returns a {@link Predicate} that always returns true.
     *
     * @param <T> Value that the predicate accepts.
     * @return Returns the predicate.
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>) ALWAYS_TRUE;
    }

    /**
     * Returns an identity function that always returns the given value.
     *
     * @param <T> Type of value to return.
     * @return Returns the identity function.
     */
    @SuppressWarnings("unchecked")
    static <T> Function<T, T> identity() {
        return (Function<T, T>) IDENTITY;
    }
}

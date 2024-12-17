/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

/**
 * Provides a way to get from an instance of T to a {@link SmithyBuilder}.
 *
 * <p>This allows modification of an otherwise immutable object using
 * the source object as a base.
 *
 * @param <T> the type that the builder will build (this)
 */
public interface ToSmithyBuilder<T> {

    /**
     * Take this object and create a builder that contains all of the
     * current property values of this object.
     *
     * @return a builder for type T
     */
    SmithyBuilder<T> toBuilder();
}

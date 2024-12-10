/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.Objects;

/**
 * A {@code Property} provides an identity-based, immutable token for a property.
 *
 * <p>The token also contains a name used to describe the value.
 */
public final class Property<T> {

    private final String name;

    /**
     * @param name Name of the value.
     */
    public Property(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Create a new identity-based property key.
     *
     * @param name Name of the property.
     * @param <T> value type associated with the property.
     * @return the created property.
     */
    public static <T> Property<T> named(String name) {
        return new Property<>(name);
    }

    @Override
    public String toString() {
        return name;
    }
}

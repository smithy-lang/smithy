/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.List;

/**
 * A type that contains tags.
 */
public interface Tagged {
    /**
     * Gets the tags applied to an object.
     *
     * @return Returns the tag values.
     */
    default List<String> getTags() {
        return ListUtils.of();
    }

    /**
     * Checks if the value has the given tag by name.
     *
     * @param tag Tag value to search for.
     * @return Returns true if the tag is present.
     */
    default boolean hasTag(String tag) {
        return getTags().contains(tag);
    }
}

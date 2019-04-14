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

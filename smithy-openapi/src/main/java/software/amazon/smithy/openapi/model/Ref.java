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

package software.amazon.smithy.openapi.model;

import java.util.Optional;
import software.amazon.smithy.model.node.ToNode;

public abstract class Ref<T extends ToNode> implements ToNode {
    /**
     * Dereferences the value stored in a ref.
     *
     * @param components Components object to query for pointers.
     * @return Returns the dereferenced value.
     */
    public abstract T deref(ComponentsObject components);

    /**
     * Gets the JSON pointer to the component.
     *
     * <p>The return value is empty if this is an inline component.
     *
     * @return Returns the optional pointer.
     */
    public abstract Optional<String> getPointer();

    /**
     * Creates a remote reference using a JSON pointer.
     *
     * @param pointer Pointer to the actual value.
     * @param <T> Type of value being created.
     * @return Returns the created Ref.
     */
    public static <T extends ToNode> Ref<T> remote(String pointer) {
        return new RemoteRef<>(pointer);
    }

    /**
     * Creates a local ref to a value that is inlined.
     *
     * @param value Inline value.
     * @param <T> Type of value that is inlined.
     * @return Returns the created Ref.
     */
    public static <T extends ToNode> Ref<T> local(T value) {
        return new LocalRef<>(value);
    }
}

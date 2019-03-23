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

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;

final class LocalRef<T extends ToNode> extends Ref<T> {
    private T value;

    LocalRef(T value) {
        this.value = value;
    }

    @Override
    public T deref(ComponentsObject components) {
        return value;
    }

    @Override
    public Optional<String> getPointer() {
        return Optional.empty();
    }

    @Override
    public Node toNode() {
        return value.toNode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof LocalRef)) {
            return false;
        }

        LocalRef ref = (LocalRef) o;
        return Objects.equals(value, ref.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

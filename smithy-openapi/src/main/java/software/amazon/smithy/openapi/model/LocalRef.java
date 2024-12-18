/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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

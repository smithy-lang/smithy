/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Contains a JMESPath expression that locates a resource identifier or property
 * value in an operation's input or output.
 */
@SmithyUnstableApi
public final class ResourceMemberBinding implements ToNode, ToSmithyBuilder<ResourceMemberBinding> {

    private final String path;

    private ResourceMemberBinding(Builder builder) {
        this.path = SmithyBuilder.requiredState("path", builder.path);
    }

    public String getPath() {
        return path;
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember("path", Node.from(path))
                .build();
    }

    public static ResourceMemberBinding fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        return builder()
                .path(obj.expectStringMember("path").getValue())
                .build();
    }

    @Override
    public Builder toBuilder() {
        return builder().path(path);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ResourceMemberBinding)) {
            return false;
        }
        return path.equals(((ResourceMemberBinding) o).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return "ResourceMemberBinding{path='" + path + "'}";
    }

    public static final class Builder implements SmithyBuilder<ResourceMemberBinding> {
        private String path;

        private Builder() {}

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        @Override
        public ResourceMemberBinding build() {
            return new ResourceMemberBinding(this);
        }
    }
}

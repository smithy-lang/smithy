/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import java.util.Objects;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An operation context parameter definition.
 */
@SmithyUnstableApi
public final class OperationContextParamDefinition implements ToNode, ToSmithyBuilder<OperationContextParamDefinition> {
    private final String path;

    private OperationContextParamDefinition(Builder builder) {
        this.path = SmithyBuilder.requiredState("path", builder.path);
    }

    public String getPath() {
        return path;
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember("path", path)
                .build();
    }

    @Override
    public Builder toBuilder() {
        return builder().path(path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OperationContextParamDefinition that = (OperationContextParamDefinition) o;
        return getPath().equals(that.getPath());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static OperationContextParamDefinition fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        Builder builder = builder();
        obj.expectStringMember("path", builder::path);
        return builder.build();
    }

    public static final class Builder implements SmithyBuilder<OperationContextParamDefinition> {
        private String path;

        private Builder() {}

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public OperationContextParamDefinition build() {
            return new OperationContextParamDefinition(this);
        }
    }
}

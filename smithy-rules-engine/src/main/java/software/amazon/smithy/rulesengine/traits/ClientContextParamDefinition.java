/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A service client context parameter definition.
 */
@SmithyUnstableApi
public final class ClientContextParamDefinition implements ToNode, ToSmithyBuilder<ClientContextParamDefinition> {
    private final ShapeType type;
    private final String documentation;

    private ClientContextParamDefinition(Builder builder) {
        this.type = SmithyBuilder.requiredState("type", builder.type);
        this.documentation = builder.documentation;
    }

    public ShapeType getType() {
        return type;
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        if (documentation != null) {
            builder.withMember("documentation", documentation);
        }
        builder.withMember("type", type.toString());
        return builder.build();
    }

    public Builder toBuilder() {
        return builder()
                .type(type)
                .documentation(documentation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getDocumentation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientContextParamDefinition that = (ClientContextParamDefinition) o;
        return getType() == that.getType() && Objects.equals(getDocumentation(), that.getDocumentation());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ClientContextParamDefinition fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        Builder builder = builder();
        obj.expectStringMember("type", s -> builder.type(ShapeType.fromString(s).orElse(null)));
        obj.getStringMember("documentation", builder::documentation);
        return builder.build();
    }

    public static final class Builder implements SmithyBuilder<ClientContextParamDefinition> {
        private ShapeType type;
        private String documentation;

        private Builder() {}

        public Builder type(ShapeType type) {
            this.type = type;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public ClientContextParamDefinition build() {
            return new ClientContextParamDefinition(this);
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.Tagged;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An enum definition for the enum trait.
 */
public final class EnumDefinition implements ToNode, ToSmithyBuilder<EnumDefinition>, Tagged {

    private final String value;
    private final String documentation;
    private final List<String> tags;
    private final String name;
    private final boolean deprecated;

    private EnumDefinition(Builder builder) {
        value = SmithyBuilder.requiredState("value", builder.value);
        name = builder.name;
        documentation = builder.documentation;
        tags = new ArrayList<>(builder.tags);
        deprecated = builder.deprecated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getValue() {
        return value;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        builder.withMember("value", getValue())
                .withOptionalMember("name", getName().map(Node::from))
                .withOptionalMember("documentation", getDocumentation().map(Node::from));

        if (!tags.isEmpty()) {
            builder.withMember("tags", Node.fromStrings(getTags()));
        }

        if (isDeprecated()) {
            builder.withMember("deprecated", true);
        }

        return builder.build();
    }

    public static EnumDefinition fromNode(Node node) {
        EnumDefinition.Builder builder = EnumDefinition.builder();
        node.expectObjectNode()
                .expectStringMember("value", builder::value)
                .getStringMember("name", builder::name)
                .getStringMember("documentation", builder::documentation)
                .getBooleanMember("deprecated", builder::deprecated)
                .getArrayMember("tags", StringNode::getValue, builder::tags);
        return builder.build();
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .name(name)
                .value(value)
                .tags(tags)
                .documentation(documentation)
                .deprecated(deprecated);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EnumDefinition)) {
            return false;
        }

        EnumDefinition otherEnum = (EnumDefinition) other;
        return value.equals(otherEnum.value)
                && Objects.equals(name, otherEnum.name)
                && Objects.equals(documentation, otherEnum.documentation)
                && tags.equals(otherEnum.tags)
                && deprecated == otherEnum.deprecated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, name, tags, documentation, deprecated);
    }

    /**
     * Builds a {@link EnumDefinition}.
     */
    public static final class Builder implements SmithyBuilder<EnumDefinition> {
        private String value;
        private String documentation;
        private String name;
        private boolean deprecated;
        private final List<String> tags = new ArrayList<>();

        @Override
        public EnumDefinition build() {
            return new EnumDefinition(this);
        }

        public Builder value(String value) {
            this.value = Objects.requireNonNull(value);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder tags(Collection<String> tags) {
            this.tags.clear();
            this.tags.addAll(tags);
            return this;
        }

        public Builder addTag(String tag) {
            tags.add(tag);
            return this;
        }

        public Builder clearTags() {
            tags.clear();
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Associates a resource with an operation lifecycle effect and optionally
 * declares where to find the resource's identifiers and properties.
 *
 * <p>Identifiers and properties may each be left unspecified, given an
 * explicit map of name to locator, or inferred from a structure named by a
 * {@code ...From} JMESPath pointer. The side each resolves against
 * (input or output) is determined per trait by the validator.
 */
@SmithyUnstableApi
public final class ResourceLifecycleBinding implements ToNode, ToSmithyBuilder<ResourceLifecycleBinding> {

    private final ShapeId resource;
    private final Map<String, ResourceMemberBinding> identifiers;
    private final String identifiersFrom;
    private final Map<String, ResourceMemberBinding> properties;
    private final String propertiesFrom;

    private ResourceLifecycleBinding(Builder builder) {
        this.resource = SmithyBuilder.requiredState("resource", builder.resource);
        this.identifiers = builder.identifiers.copy();
        this.identifiersFrom = builder.identifiersFrom;
        this.properties = builder.properties.copy();
        this.propertiesFrom = builder.propertiesFrom;
    }

    public ShapeId getResource() {
        return resource;
    }

    public Map<String, ResourceMemberBinding> getIdentifiers() {
        return identifiers;
    }

    public Optional<String> getIdentifiersFrom() {
        return Optional.ofNullable(identifiersFrom);
    }

    public Map<String, ResourceMemberBinding> getProperties() {
        return properties;
    }

    public Optional<String> getPropertiesFrom() {
        return Optional.ofNullable(propertiesFrom);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember("resource", Node.from(resource.toString()));
        if (!identifiers.isEmpty()) {
            builder.withMember("identifiers", membersToNode(identifiers));
        }
        if (identifiersFrom != null) {
            builder.withMember("identifiersFrom", Node.from(identifiersFrom));
        }
        if (!properties.isEmpty()) {
            builder.withMember("properties", membersToNode(properties));
        }
        if (propertiesFrom != null) {
            builder.withMember("propertiesFrom", Node.from(propertiesFrom));
        }
        return builder.build();
    }

    private static Node membersToNode(Map<String, ResourceMemberBinding> members) {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        for (Map.Entry<String, ResourceMemberBinding> entry : members.entrySet()) {
            builder.withMember(entry.getKey(), entry.getValue().toNode());
        }
        return builder.build();
    }

    public static ResourceLifecycleBinding fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        Builder builder = builder();
        builder.resource(ShapeId.from(obj.expectStringMember("resource").getValue()));
        obj.getObjectMember("identifiers")
                .ifPresent(ids -> ids.getMembers()
                        .forEach((key, value) -> builder.putIdentifier(key.getValue(),
                                ResourceMemberBinding.fromNode(value))));
        obj.getStringMember("identifiersFrom").ifPresent(from -> builder.identifiersFrom(from.getValue()));
        obj.getObjectMember("properties")
                .ifPresent(props -> props.getMembers()
                        .forEach((key, value) -> builder.putProperty(key.getValue(),
                                ResourceMemberBinding.fromNode(value))));
        obj.getStringMember("propertiesFrom").ifPresent(from -> builder.propertiesFrom(from.getValue()));
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ResourceLifecycleBinding)) {
            return false;
        }
        ResourceLifecycleBinding that = (ResourceLifecycleBinding) o;
        return resource.equals(that.resource)
                && identifiers.equals(that.identifiers)
                && Objects.equals(identifiersFrom, that.identifiersFrom)
                && properties.equals(that.properties)
                && Objects.equals(propertiesFrom, that.propertiesFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, identifiers, identifiersFrom, properties, propertiesFrom);
    }

    @Override
    public String toString() {
        return "ResourceLifecycleBinding{resource=" + resource
                + ", identifiers=" + identifiers
                + ", identifiersFrom=" + identifiersFrom
                + ", properties=" + properties
                + ", propertiesFrom=" + propertiesFrom + "}";
    }

    public static final class Builder implements SmithyBuilder<ResourceLifecycleBinding> {
        private ShapeId resource;
        private final BuilderRef<Map<String, ResourceMemberBinding>> identifiers = BuilderRef.forOrderedMap();
        private String identifiersFrom;
        private final BuilderRef<Map<String, ResourceMemberBinding>> properties = BuilderRef.forOrderedMap();
        private String propertiesFrom;

        private Builder() {}

        private Builder(ResourceLifecycleBinding binding) {
            this.resource = binding.resource;
            this.identifiers.setBorrowed(binding.identifiers);
            this.identifiersFrom = binding.identifiersFrom;
            this.properties.setBorrowed(binding.properties);
            this.propertiesFrom = binding.propertiesFrom;
        }

        public Builder resource(ShapeId resource) {
            this.resource = Objects.requireNonNull(resource);
            return this;
        }

        public Builder identifiers(Map<String, ResourceMemberBinding> identifiers) {
            this.identifiers.clear();
            this.identifiers.get().putAll(Objects.requireNonNull(identifiers));
            return this;
        }

        public Builder putIdentifier(String name, ResourceMemberBinding binding) {
            this.identifiers.get().put(name, binding);
            return this;
        }

        public Builder identifiersFrom(String identifiersFrom) {
            this.identifiersFrom = identifiersFrom;
            return this;
        }

        public Builder properties(Map<String, ResourceMemberBinding> properties) {
            this.properties.clear();
            this.properties.get().putAll(Objects.requireNonNull(properties));
            return this;
        }

        public Builder putProperty(String name, ResourceMemberBinding binding) {
            this.properties.get().put(name, binding);
            return this;
        }

        public Builder propertiesFrom(String propertiesFrom) {
            this.propertiesFrom = propertiesFrom;
            return this;
        }

        @Override
        public ResourceLifecycleBinding build() {
            return new ResourceLifecycleBinding(this);
        }
    }
}

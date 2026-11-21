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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Associates a resource with an operation that creates or replaces it with put
 * semantics, and optionally declares where to find the resource's identifiers
 * and properties.
 *
 * <p>Identical to {@link ResourceLifecycleBinding}, plus {@code noReplace},
 * which indicates that the operation can only create the resource and cannot
 * replace an existing one. It is the binding-level counterpart of the
 * {@link NoReplaceTrait}, which applies to a resource's put lifecycle operation.
 */
@SmithyUnstableApi
public final class ResourcePutBinding implements ResourcePropertiesBinding, ToSmithyBuilder<ResourcePutBinding> {

    private final ShapeId resource;
    private final Map<String, ResourceMemberBinding> identifiers;
    private final String identifiersFrom;
    private final Map<String, ResourceMemberBinding> properties;
    private final String propertiesFrom;
    private final boolean noReplace;

    private ResourcePutBinding(Builder builder) {
        this.resource = SmithyBuilder.requiredState("resource", builder.resource);
        this.identifiers = builder.identifiers.copy();
        this.identifiersFrom = builder.identifiersFrom;
        this.properties = builder.properties.copy();
        this.propertiesFrom = builder.propertiesFrom;
        this.noReplace = builder.noReplace;
    }

    @Override
    public ShapeId getResource() {
        return resource;
    }

    @Override
    public Map<String, ResourceMemberBinding> getIdentifiers() {
        return identifiers;
    }

    @Override
    public Optional<String> getIdentifiersFrom() {
        return Optional.ofNullable(identifiersFrom);
    }

    @Override
    public Map<String, ResourceMemberBinding> getProperties() {
        return properties;
    }

    @Override
    public Optional<String> getPropertiesFrom() {
        return Optional.ofNullable(propertiesFrom);
    }

    /**
     * @return Returns true if the operation can only create the resource and cannot replace an existing one.
     */
    public boolean isNoReplace() {
        return noReplace;
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
        if (noReplace) {
            builder.withMember("noReplace", Node.from(true));
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

    public static ResourcePutBinding fromNode(Node node) {
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
        obj.getBooleanMember("noReplace").ifPresent(value -> builder.noReplace(value.getValue()));
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
        } else if (!(o instanceof ResourcePutBinding)) {
            return false;
        }
        ResourcePutBinding that = (ResourcePutBinding) o;
        return resource.equals(that.resource)
                && identifiers.equals(that.identifiers)
                && Objects.equals(identifiersFrom, that.identifiersFrom)
                && properties.equals(that.properties)
                && Objects.equals(propertiesFrom, that.propertiesFrom)
                && noReplace == that.noReplace;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, identifiers, identifiersFrom, properties, propertiesFrom, noReplace);
    }

    @Override
    public String toString() {
        return "ResourcePutBinding{resource=" + resource
                + ", identifiers=" + identifiers
                + ", identifiersFrom=" + identifiersFrom
                + ", properties=" + properties
                + ", propertiesFrom=" + propertiesFrom
                + ", noReplace=" + noReplace + "}";
    }

    public static final class Builder implements SmithyBuilder<ResourcePutBinding> {
        private ShapeId resource;
        private final BuilderRef<Map<String, ResourceMemberBinding>> identifiers = BuilderRef.forOrderedMap();
        private String identifiersFrom;
        private final BuilderRef<Map<String, ResourceMemberBinding>> properties = BuilderRef.forOrderedMap();
        private String propertiesFrom;
        private boolean noReplace;

        private Builder() {}

        private Builder(ResourcePutBinding binding) {
            this.resource = binding.resource;
            this.identifiers.setBorrowed(binding.identifiers);
            this.identifiersFrom = binding.identifiersFrom;
            this.properties.setBorrowed(binding.properties);
            this.propertiesFrom = binding.propertiesFrom;
            this.noReplace = binding.noReplace;
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

        public Builder noReplace(boolean noReplace) {
            this.noReplace = noReplace;
            return this;
        }

        @Override
        public ResourcePutBinding build() {
            return new ResourcePutBinding(this);
        }
    }
}

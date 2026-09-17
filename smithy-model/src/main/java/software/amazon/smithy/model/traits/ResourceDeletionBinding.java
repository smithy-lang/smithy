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
 * Associates a deleted resource with an operation and optionally declares where
 * to find the resource's identifiers.
 *
 * <p>Unlike {@link ResourceLifecycleBinding}, a deletion binding has no
 * properties: a resource is deleted by its identifiers alone.
 */
@SmithyUnstableApi
public final class ResourceDeletionBinding implements ResourceBinding, ToSmithyBuilder<ResourceDeletionBinding> {

    private final ShapeId resource;
    private final Map<String, ResourceMemberBinding> identifiers;
    private final String identifiersFrom;

    private ResourceDeletionBinding(Builder builder) {
        this.resource = SmithyBuilder.requiredState("resource", builder.resource);
        this.identifiers = builder.identifiers.copy();
        this.identifiersFrom = builder.identifiersFrom;
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
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember("resource", Node.from(resource.toString()));
        if (!identifiers.isEmpty()) {
            builder.withMember("identifiers", membersToNode(identifiers));
        }
        if (identifiersFrom != null) {
            builder.withMember("identifiersFrom", Node.from(identifiersFrom));
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

    public static ResourceDeletionBinding fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        Builder builder = builder();
        builder.resource(ShapeId.from(obj.expectStringMember("resource").getValue()));
        obj.getObjectMember("identifiers")
                .ifPresent(ids -> ids.getMembers()
                        .forEach((key, value) -> builder.putIdentifier(key.getValue(),
                                ResourceMemberBinding.fromNode(value))));
        obj.getStringMember("identifiersFrom").ifPresent(from -> builder.identifiersFrom(from.getValue()));
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
        } else if (!(o instanceof ResourceDeletionBinding)) {
            return false;
        }
        ResourceDeletionBinding that = (ResourceDeletionBinding) o;
        return resource.equals(that.resource)
                && identifiers.equals(that.identifiers)
                && Objects.equals(identifiersFrom, that.identifiersFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, identifiers, identifiersFrom);
    }

    @Override
    public String toString() {
        return "ResourceDeletionBinding{resource=" + resource
                + ", identifiers=" + identifiers
                + ", identifiersFrom=" + identifiersFrom + "}";
    }

    public static final class Builder implements SmithyBuilder<ResourceDeletionBinding> {
        private ShapeId resource;
        private final BuilderRef<Map<String, ResourceMemberBinding>> identifiers = BuilderRef.forOrderedMap();
        private String identifiersFrom;

        private Builder() {}

        private Builder(ResourceDeletionBinding binding) {
            this.resource = binding.resource;
            this.identifiers.setBorrowed(binding.identifiers);
            this.identifiersFrom = binding.identifiersFrom;
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

        @Override
        public ResourceDeletionBinding build() {
            return new ResourceDeletionBinding(this);
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.validators.ReferencesTraitValidator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines references to resources within a structure.
 *
 * @see ReferencesTraitValidator
 */
public final class ReferencesTrait extends AbstractTrait implements ToSmithyBuilder<ReferencesTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#references");

    private final List<Reference> references;

    private ReferencesTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.references = ListUtils.copyOf(builder.references);
    }

    /**
     * Gets the references.
     *
     * @return Returns the unmodifiable list of references.
     */
    public List<Reference> getReferences() {
        return references;
    }

    /**
     * Gets a list of all references to a particular shape.
     *
     * @param shapeId Shape ID to search for.
     *
     * @return Returns the list of found references.
     */
    public List<Reference> getResourceReferences(ShapeId shapeId) {
        return getReferences().stream()
                .filter(reference -> reference.getResource().equals(shapeId))
                .collect(Collectors.toList());
    }

    @Override
    protected Node createNode() {
        return references.stream().map(Reference::toNode).collect(ArrayNode.collect(getSourceLocation()));
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder().sourceLocation(getSourceLocation());
        references.forEach(builder::addReference);
        return builder;
    }

    // Ignore inconsequential toNode differences.
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReferencesTrait)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            ReferencesTrait trait = (ReferencesTrait) other;
            return references.equals(trait.references);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(toShapeId(), references);
    }

    /**
     * @return Returns a builder used to create a references trait.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder use to create the references trait.
     */
    public static final class Builder extends AbstractTraitBuilder<ReferencesTrait, Builder> {
        private final List<Reference> references = new ArrayList<>();

        public Builder addReference(Reference reference) {
            references.add(Objects.requireNonNull(reference));
            return this;
        }

        public Builder clearReferences() {
            references.clear();
            return this;
        }

        public Builder references(List<Reference> references) {
            this.references.clear();
            this.references.addAll(references);
            return this;
        }

        @Override
        public ReferencesTrait build() {
            return new ReferencesTrait(this);
        }
    }

    /**
     * Reference to a resource.
     */
    public static final class Reference implements ToSmithyBuilder<Reference>, ToNode {
        private final ShapeId resource;
        private final Map<String, String> ids;
        private final ShapeId service;
        private final String rel;

        private Reference(Builder builder) {
            resource = SmithyBuilder.requiredState("resource", builder.resource);
            ids = Collections.unmodifiableMap(new TreeMap<>(builder.ids));
            rel = builder.rel;
            service = builder.service;
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public Builder toBuilder() {
            return builder().resource(resource).ids(ids).service(service).rel(rel);
        }

        /**
         * Get the referenced shape.
         *
         * @return Returns the referenced shape.
         */
        public ShapeId getResource() {
            return resource;
        }

        /**
         * Get the service binding.
         *
         * @return Returns the optionally referenced service.
         */
        public Optional<ShapeId> getService() {
            return Optional.ofNullable(service);
        }

        /**
         * @return Returns the immutable mapping of member names to resource identifier name.
         */
        public Map<String, String> getIds() {
            return ids;
        }

        /**
         * @return Gets the optional rel property.
         */
        public Optional<String> getRel() {
            return Optional.ofNullable(rel);
        }

        @Override
        public String toString() {
            return "Reference" + Node.printJson(toNode());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof Reference)) {
                return false;
            }

            Reference reference = (Reference) o;
            return resource.equals(reference.resource)
                    && Objects.equals(ids, reference.ids)
                    && Objects.equals(service, reference.service)
                    && Objects.equals(rel, reference.rel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource, ids, service, rel);
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember("resource", Node.from(resource.toString()))
                    .withOptionalMember("ids",
                            ids.isEmpty()
                                    ? Optional.empty()
                                    : Optional.of(ObjectNode.fromStringMap(getIds())))
                    .withOptionalMember("service", getService().map(ShapeId::toString).map(Node::from))
                    .withOptionalMember("rel", getRel().map(Node::from))
                    .build();
        }

        /**
         * Builder to create a {@link Reference}.
         */
        public static final class Builder implements SmithyBuilder<Reference> {
            private ShapeId resource;
            private String rel;
            private Map<String, String> ids = MapUtils.of();
            private ShapeId service;

            private Builder() {}

            @Override
            public Reference build() {
                return new Reference(this);
            }

            public Builder ids(Map<String, String> members) {
                this.ids = Objects.requireNonNull(members);
                return this;
            }

            public Builder resource(ShapeId resource) {
                this.resource = Objects.requireNonNull(resource);
                return this;
            }

            public Builder service(ShapeId service) {
                this.service = service;
                return this;
            }

            public Builder rel(String rel) {
                this.rel = rel;
                return this;
            }
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public ReferencesTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ArrayNode refs = value.expectArrayNode();
            for (ObjectNode member : refs.getElementsAs(ObjectNode.class)) {
                builder.addReference(referenceFromNode(member));
            }
            ReferencesTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }

        private static Reference referenceFromNode(ObjectNode referenceProperties) {
            Reference.Builder builder = Reference.builder();
            referenceProperties.expectObjectNode()
                    .expectMember("resource", ShapeId::fromNode, builder::resource)
                    .getObjectMember("ids", object -> {
                        Map<String, String> result = new LinkedHashMap<>(object.size());
                        object.getStringMap().forEach((k, v) -> {
                            result.put(k, v.expectStringNode().getValue());
                        });
                        builder.ids(result);
                    })
                    .getMember("service", ShapeId::fromNode, builder::service)
                    .getStringMember("rel", builder::rel);
            return builder.build();
        }
    }
}

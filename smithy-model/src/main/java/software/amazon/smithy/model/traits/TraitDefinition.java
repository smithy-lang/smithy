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

package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.Tagged;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Trait definition.
 */
public final class TraitDefinition implements ToNode, FromSourceLocation, Tagged, ToSmithyBuilder<TraitDefinition> {
    public static final String SELECTOR_KEY = "selector";
    public static final String STRUCTURALLY_EXCLUSIVE_KEY = "structurallyExclusive";
    public static final String SHAPE_KEY = "shape";
    public static final String TAGS_KEY = "tags";
    public static final String CONFLICTS_KEY = "conflicts";
    public static final String DOCUMENTATION_KEY = "documentation";
    public static final String EXTERNAL_DOCUMENTATION_KEY = "externalDocumentation";
    public static final String DEPRECATED_KEY = "deprecated";
    public static final String DEPRECATION_REASON_KEY = "deprecationReason";

    private final String traitName;
    private final String traitNamespace;
    private final String documentation;
    private final String externalDocumentation;
    private final SourceLocation sourceLocation;
    private final ShapeId shape;
    private final Selector selector;
    private final List<String> tags;
    private final List<String> conflicts;
    private final boolean structurallyExclusive;
    private final boolean deprecated;
    private final String deprecationReason;

    public TraitDefinition(TraitDefinition.Builder builder) {
        SmithyBuilder.requiredState("traitName", builder.traitName);
        traitNamespace = builder.traitName.substring(0, builder.traitName.indexOf("#"));
        traitName =  builder.traitName.substring(builder.traitName.indexOf("#") + 1);
        sourceLocation = builder.sourceLocation;
        shape = builder.shape;
        selector = builder.selector;
        tags = ListUtils.copyOf(builder.tags);
        conflicts = ListUtils.copyOf(builder.conflicts);
        structurallyExclusive = builder.structurallyExclusive;
        documentation = builder.documentation;
        externalDocumentation = builder.externalDocumentation;
        deprecated = builder.deprecated;
        deprecationReason = builder.deprecationReason;

        if (deprecationReason != null && !deprecated) {
            throw new SourceException("deprecationReason cannot be set if deprecated is not set", getSourceLocation());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .name(traitNamespace + "#" + traitName)
                .sourceLocation(sourceLocation)
                .shape(shape)
                .selector(selector)
                .documentation(documentation)
                .externalDocumentation(externalDocumentation)
                .structurallyExclusive(structurallyExclusive)
                .deprecated(deprecated)
                .deprecationReason(deprecationReason);
        tags.forEach(builder::addTag);
        conflicts.forEach(builder::addConflict);
        return builder;
    }

    /**
     * Gets the fully qualified name of the trait, including namespace.
     *
     * @return Returns the name + "#" + namespace.
     */
    public String getFullyQualifiedName() {
        return getNamespace() + "#" + getName();
    }

    /**
     * Gets the name of the trait, without the namespace.
     * @return Returns the trait name.
     */
    public String getName() {
        return traitName;
    }

    /**
     * Gets the namespace of the trait.
     *
     * @return Returns the namespace.
     */
    public String getNamespace() {
        return traitNamespace;
    }

    /**
     * Get the shape that defines the format values that the trait can store.
     *
     * @return Returns the optional shape.
     */
    public Optional<ShapeId> getShape() {
        return Optional.ofNullable(shape);
    }

    /**
     * Get whether or not the trait is an annotation trait, meaning it can
     * only be set to true.
     *
     * @return Returns true if an annotation trait.
     */
    public boolean isAnnotationTrait() {
        return shape == null;
    }

    /**
     * Gets the valid places in a model that this trait can be applied.
     *
     * @return Returns the trait selector.
     */
    public Selector getSelector() {
        return selector;
    }

    /**
     * Gets the trait names that conflict with this trait.
     *
     * @return Returns the conflicting trait names.
     */
    public List<String> getConflicts() {
        return conflicts;
    }

    /**
     * @return Returns true if the trait is structurally exclusive.
     */
    public boolean isStructurallyExclusive() {
        return structurallyExclusive;
    }

    /**
     * @return Returns true if the trait is deprecated.
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * @return Gets the reason why the trait is deprecated.
     */
    public Optional<String> getDeprecationReason() {
        return Optional.ofNullable(deprecationReason);
    }

    /**
     * @return Returns the optionally present documentation.
     */
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * @return Returns the optionally present external documentation link.
     */
    public Optional<String> getExternalDocumentation() {
        return Optional.ofNullable(externalDocumentation);
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder().sourceLocation(getSourceLocation());

        if (selector != Selector.IDENTITY) {
            builder.withMember(SELECTOR_KEY, selector.toString());
        }

        if (shape != null) {
            builder.withMember(SHAPE_KEY, shape.toString());
        }

        if (!conflicts.isEmpty()) {
            builder.withMember(CONFLICTS_KEY, conflicts.stream().map(Node::from).collect(ArrayNode.collect()));
        }

        if (isStructurallyExclusive()) {
            builder.withMember(STRUCTURALLY_EXCLUSIVE_KEY, Node.from(true));
        }

        getDocumentation().ifPresent(value -> builder.withMember(DOCUMENTATION_KEY, value));
        getExternalDocumentation().ifPresent(value -> builder.withMember(EXTERNAL_DOCUMENTATION_KEY, value));

        if (!getTags().isEmpty()) {
            builder.withMember(TAGS_KEY, Node.fromStrings(getTags()));
        }

        if (isDeprecated()) {
            builder.withMember(DEPRECATED_KEY, Node.from(true));
            getDeprecationReason().ifPresent(value -> builder.withMember(DEPRECATION_REASON_KEY, value));
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return String.format("trait definition `%s`", getFullyQualifiedName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final TraitDefinition that = (TraitDefinition) o;
            return traitName.equals(that.traitName)
                   && traitNamespace.equals(that.traitNamespace)
                   && getShape().equals(that.getShape())
                   && selector.toString().equals(that.selector.toString())
                   && tags.equals(that.tags)
                   && structurallyExclusive == that.structurallyExclusive
                   && deprecated == that.deprecated
                   && conflicts.equals(that.conflicts)
                   && Objects.equals(deprecationReason, that.deprecationReason)
                   && Objects.equals(documentation, that.documentation)
                   && Objects.equals(externalDocumentation, that.externalDocumentation);
        }
    }

    @Override
    public int hashCode() {
        return getFullyQualifiedName().hashCode();
    }

    /**
     * Builder to create a TraitDefinition.
     */
    public static final class Builder implements SmithyBuilder<TraitDefinition>, FromSourceLocation {
        private String traitName;
        private SourceLocation sourceLocation = SourceLocation.none();
        private String documentation;
        private String externalDocumentation;
        private ShapeId shape;
        private Selector selector = Selector.IDENTITY;
        private final List<String> tags = new ArrayList<>();
        private final List<String> conflicts = new ArrayList<>();
        private boolean structurallyExclusive;
        private boolean deprecated;
        private String deprecationReason;

        private Builder() {}

        @Override
        public SourceLocation getSourceLocation() {
            return sourceLocation;
        }

        public String getFullyQualifiedName() {
            return traitName;
        }

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Objects.requireNonNull(sourceLocation);
            return this;
        }

        public Builder shape(ShapeId shape) {
            this.shape = shape;
            return this;
        }

        public Builder selector(Selector selector) {
            this.selector = selector;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder externalDocumentation(String externalDocumentation) {
            this.externalDocumentation = externalDocumentation;
            return this;
        }

        public Builder addConflict(String trait) {
            Objects.requireNonNull(trait);

            // Use absolute trait names.
            if (!trait.contains("#")) {
                trait = Prelude.NAMESPACE + "#" + trait;
            }

            conflicts.add(trait);
            return this;
        }

        public Builder removeConflict(String trait) {
            conflicts.remove(trait);
            return this;
        }

        public Builder addTag(String tag) {
            tags.add(Objects.requireNonNull(tag));
            return this;
        }

        public Builder removeTag(String tag) {
            tags.remove(tag);
            return this;
        }

        public Builder clearTags() {
            tags.clear();
            return this;
        }

        public Builder structurallyExclusive(boolean structurallyExclusive) {
            this.structurallyExclusive = structurallyExclusive;
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder deprecationReason(String deprecationReason) {
            this.deprecationReason = deprecationReason;
            return this;
        }

        /**
         * Sets the fully qualified trait name (e.g., "namespace#name").
         *
         * @param traitName Fully qualified name to set.
         * @return Returns the builder.
         */
        public Builder name(String traitName) {
            this.traitName = Objects.requireNonNull(traitName);

            if (traitName.indexOf("#") <= 0) {
                throw new IllegalArgumentException(
                        "Invalid trait definition name. Expected a namespace, found " + traitName);
            }

            return this;
        }

        @Override
        public TraitDefinition build() {
            return new TraitDefinition(this);
        }
    }
}

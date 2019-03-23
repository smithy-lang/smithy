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

package software.amazon.smithy.model;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.ValidatorFactory;

/**
 * A Smithy model, including shapes, traits, custom traits, validators,
 * suppressions and metadata.
 *
 * <p>A "model" contains all of the loaded shapes (including their traits),
 * validator definitions, suppression definitions, metadata properties,
 * the Smithy version number, and all custom trait definitions. Each of
 * these properties of a model contain a {@link SourceLocation}, allowing
 * a reference back to where they were defined and how they were originally
 * grouped into separate document files.
 */
public final class Model implements ToSmithyBuilder<Model> {
    private final Map<String, Node> metadata;
    private final ShapeIndex shapeIndex;
    private final Map<String, TraitDefinition> traitDefinitions;
    private final String smithyVersion;

    /** Cache of computed {@link KnowledgeIndex} instances. */
    private final Map<Class<? extends KnowledgeIndex>, KnowledgeIndex> blackboard = new ConcurrentHashMap<>();

    /** Lazily computed hashcode. */
    private int hash;

    private Model(Builder builder) {
        smithyVersion = builder.smithyVersion;
        shapeIndex = builder.shapeIndex != null ? builder.shapeIndex : ShapeIndex.builder().build();
        metadata = builder.metadata.isEmpty() ? Map.of() : Map.copyOf(builder.metadata);
        traitDefinitions = builder.traitDefinitions.isEmpty() ? Map.of() : builder.traitDefinitions.stream()
                .collect(toUnmodifiableMap(TraitDefinition::getFullyQualifiedName, identity(), (a, b) -> b));
    }

    /**
     * Builds an explicitly configured Smithy model.
     *
     * <p>Note that the builder does not validate the correctness of the
     * model. Use the {@link #assembler()} method to build <em>and</em>
     * validate a model.
     *
     * @return Returns a model builder.
     * @see #assembler()
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Assembles and validates a Smithy model from files, nodes, and other
     * disparate sources.
     *
     * @return Returns a model assembler.
     */
    public static ModelAssembler assembler() {
        return new ModelAssembler();
    }

    /**
     * Creates a {@link ModelAssembler} that is configured to discover traits,
     * validators, and built-in validators using the given
     * {@code ClassLoader}.
     *
     * @param classLoader Class loader used to discover services.
     * @return Returns a model assembler.
     */
    public static ModelAssembler assembler(ClassLoader classLoader) {
        return new ModelAssembler()
                .traitFactory(TraitFactory.createServiceFactory(classLoader))
                .validatorFactory(ValidatorFactory.createServiceFactory(classLoader));
    }

    /**
     * Creates a {@link ModelAssembler} that is configured to discover traits,
     * validators, and built-in validators using the given {@code ModuleLayer}.
     *
     * @param moduleLayer ModuleLayer used to discover services.
     * @return Returns a model assembler.
     */
    public static ModelAssembler assembler(ModuleLayer moduleLayer) {
        return new ModelAssembler()
                .traitFactory(TraitFactory.createServiceFactory(moduleLayer))
                .validatorFactory(ValidatorFactory.createServiceFactory(moduleLayer));
    }

    /**
     * Gets the {@link ShapeIndex} of the {@code Model}.
     *
     * @return Returns the index of shapes.
     */
    public ShapeIndex getShapeIndex() {
        return shapeIndex;
    }

    /**
     * Gets a metadata property by namespace and name.
     *
     * @param name Name of the property to retrieve.
     * @return Returns the optional property.
     */
    public Optional<Node> getMetadataProperty(String name) {
        return Optional.ofNullable(metadata.get(name));
    }

    /**
     * @return Gets the unmodifiable metadata of the model across all
     *  namespaces.
     */
    public Map<String, Node> getMetadata() {
        return metadata;
    }

    public String getSmithyVersion() {
        return smithyVersion;
    }

    /**
     * @return Returns all trait definitions in the model.
     */
    public Set<TraitDefinition> getTraitDefinitions() {
        return new HashSet<>(traitDefinitions.values());
    }

    /**
     * Get a trait definition by fully qualified name.
     *
     * @param traitName Trait definition name to retrieve.
     * @return Returns the optionally found definition.
     */
    public Optional<TraitDefinition> getTraitDefinition(String traitName) {
        String resolved = Trait.makeAbsoluteName(traitName);
        return Optional.ofNullable(traitDefinitions.get(resolved));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Model)) {
            return false;
        } else if (other == this) {
            return true;
        }

        Model otherModel = (Model) other;
        return getSmithyVersion().equals(otherModel.getSmithyVersion())
               && getMetadata().equals(otherModel.getMetadata())
               && getTraitDefinitions().equals(otherModel.getTraitDefinitions())
               && getShapeIndex().equals(otherModel.getShapeIndex());
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            result = Objects.hash(getSmithyVersion(), getMetadata(), getTraitDefinitions(), getShapeIndex());
            hash = result;
        }
        return result;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .smithyVersion(smithyVersion)
                .metadata(getMetadata())
                .shapeIndex(getShapeIndex());
        traitDefinitions.values().forEach(builder::addTraitDefinition);
        return builder;
    }

    /**
     * Gets a computed knowledge index of a specific type for the model.
     *
     * <p>If a {@link KnowledgeIndex} of the given type has not yet been
     * computed, one will be created using a constructor of the given type
     * that accepts a {@link Model}. Computed knowledge indexes are cached
     * and returned on subsequent retrievals.
     *
     * <p>Using this method is preferred over directly instantiating instances
     * of a KnowledgeIndex if the KnowledgeIndex required in various unrelated
     * code paths where passing around an instance of a KnowledgeIndex is not
     * practical or impossible.
     *
     * @param type Type of knowledge index to retrieve.
     * @param <T> The type of knowledge index to retrieve.
     * @return Returns the computed knowledge index.
     */
    @SuppressWarnings("unchecked")
    public <T extends KnowledgeIndex> T getKnowledge(Class<T> type) {
        // This method intentionally does not use putIfAbsent to avoid
        // deadlocks in the case where a knowledge index needs to access
        // other knowledge indexes from a Model. While this *can* cause
        // duplicate computations, it's preferable over *always* requiring
        // duplicate computations, deadlocks of computeIfAbsent, or
        // spreading out the cache state associated with previously
        // computed indexes through WeakHashMaps on each KnowledgeIndex
        // (a previous approach we used for caching).
        T value = (T) blackboard.get(type);

        if (value == null) {
            value = KnowledgeIndex.create(type, this);
            blackboard.put(type, value);
        }

        return value;
    }

    /**
     * Builder used to create a Model.
     */
    public static final class Builder implements SmithyBuilder<Model> {
        private Map<String, Node> metadata = new HashMap<>();
        private Set<TraitDefinition> traitDefinitions = new HashSet<>();
        private String smithyVersion = "1.0";
        private ShapeIndex shapeIndex;

        private Builder() {}

        public Builder smithyVersion(String smithyVersion) {
            this.smithyVersion = smithyVersion;
            return this;
        }

        public Builder metadata(Map<String, Node> metadata) {
            clearMetadata();
            this.metadata.putAll(metadata);
            return this;
        }

        public Builder putMetadataProperty(String key, Node value) {
            metadata.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
            return this;
        }

        public Builder clearMetadata() {
            metadata.clear();
            return this;
        }

        public Builder shapeIndex(ShapeIndex shapeIndex) {
            this.shapeIndex = Objects.requireNonNull(shapeIndex);
            return this;
        }

        public Builder addTraitDefinition(TraitDefinition traitDefinition) {
            traitDefinitions.add(Objects.requireNonNull(traitDefinition));
            return this;
        }

        public Builder addTraitDefinitions(Collection<TraitDefinition> traitDefinitions) {
            this.traitDefinitions.addAll(traitDefinitions);
            return this;
        }

        public Builder clearTraitDefinitions() {
            traitDefinitions.clear();
            return this;
        }

        @Override
        public Model build() {
            return new Model(this);
        }
    }
}

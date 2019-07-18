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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

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
    /** Specifies the highest supported version of the IDL. */
    public static final String MODEL_VERSION = "0.3.0";

    private final Map<String, Node> metadata;
    private final ShapeIndex shapeIndex;
    private final String smithyVersion;

    private volatile Map<Shape, TraitDefinition> traitDefinitions;

    /** Cache of computed {@link KnowledgeIndex} instances. */
    private final Map<Class<? extends KnowledgeIndex>, KnowledgeIndex> blackboard = new ConcurrentHashMap<>();

    /** Lazily computed hashcode. */
    private int hash;

    private Model(Builder builder) {
        smithyVersion = builder.smithyVersion;
        shapeIndex = builder.shapeIndex != null ? builder.shapeIndex : ShapeIndex.builder().build();
        metadata = builder.metadata.isEmpty() ? MapUtils.of() : MapUtils.copyOf(builder.metadata);
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
     * Returns are trait shapes in the model as a map of {@code Shape}
     * objects to their {@link TraitDefinition} traits.
     *
     * @return Returns all trait definitions in the model.
     */
    public Map<Shape, TraitDefinition> getTraitDefinitions() {
        if (traitDefinitions == null) {
            traitDefinitions = Collections.unmodifiableMap(shapeIndex.shapes()
                    .flatMap(shape -> Trait.flatMapStream(shape, TraitDefinition.class))
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));
        }

        return traitDefinitions;
    }

    /**
     * Gets the trait definition of a specific trait shape ID.
     *
     * @param traitId ID of the shape to get the trait definition of.
     * @return Returns the optionally found trait definition.
     */
    public Optional<TraitDefinition> getTraitDefinition(ToShapeId traitId) {
        return shapeIndex.getShape(traitId.toShapeId()).flatMap(shape -> shape.getTrait(TraitDefinition.class));
    }

    /**
     * Gets the trait definition of a specific trait shape ID.
     *
     * @param traitId ID of the shape to get the trait definition of.
     * @return Returns the optionally found trait definition.
     */
    public Optional<TraitDefinition> getTraitDefinition(String traitId) {
        return getTraitDefinition(ShapeId.from(Trait.makeAbsoluteName(traitId)));
    }

    /**
     * @return Returns all trait definition shapes in the model.
     */
    public Collection<Shape> getTraitShapes() {
        return getTraitDefinitions().keySet();
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
               && getShapeIndex().equals(otherModel.getShapeIndex());
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            result = Objects.hash(getSmithyVersion(), getMetadata(), getShapeIndex());
            hash = result;
        }
        return result;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .smithyVersion(smithyVersion)
                .metadata(getMetadata())
                .shapeIndex(getShapeIndex());
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
        private String smithyVersion = MODEL_VERSION;
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

        @Override
        public Model build() {
            return new Model(this);
        }
    }
}

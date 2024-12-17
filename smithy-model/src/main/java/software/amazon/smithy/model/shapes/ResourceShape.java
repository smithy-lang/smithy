/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code resource} shape.
 */
public final class ResourceShape extends EntityShape implements ToSmithyBuilder<ResourceShape> {
    private final Map<String, ShapeId> identifiers;
    private final Map<String, ShapeId> properties;
    private final ShapeId put;
    private final ShapeId create;
    private final ShapeId read;
    private final ShapeId update;
    private final ShapeId delete;
    private final ShapeId list;
    private final Set<ShapeId> collectionOperations;
    private final Set<ShapeId> allOperations = new HashSet<>();

    private ResourceShape(Builder builder) {
        super(builder);
        identifiers = builder.identifiers.copy();
        properties = builder.properties.copy();
        put = builder.put;
        create = builder.create;
        read = builder.read;
        update = builder.update;
        delete = builder.delete;
        list = builder.list;
        collectionOperations = builder.collectionOperations.copy();

        // Compute all operations bound to the resource.
        allOperations.addAll(getOperations());
        allOperations.addAll(getCollectionOperations());
        getPut().ifPresent(allOperations::add);
        getCreate().ifPresent(allOperations::add);
        getRead().ifPresent(allOperations::add);
        getUpdate().ifPresent(allOperations::add);
        getDelete().ifPresent(allOperations::add);
        getList().ifPresent(allOperations::add);

        if (hasTrait(MixinTrait.ID) && (!getIdentifiers().isEmpty()
                || getPut().isPresent()
                || getCreate().isPresent()
                || getRead().isPresent()
                || getUpdate().isPresent()
                || getDelete().isPresent()
                || getList().isPresent()
                || !getProperties().isEmpty()
                || !getOperations().isEmpty()
                || !getResources().isEmpty())) {
            throw new IllegalStateException(String.format(
                    "Resource shapes with the mixin trait may not define any properties. Resource mixin shape `%s` "
                            + "defines one or more properties.",
                    getId()));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = updateBuilder(builder())
                .identifiers(getIdentifiers())
                .properties(properties)
                .put(put)
                .create(create)
                .read(read)
                .update(update)
                .delete(delete)
                .list(list);
        getOperations().forEach(builder::addOperation);
        getCollectionOperations().forEach(builder::addCollectionOperation);
        getResources().forEach(builder::addResource);
        return builder;
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.resourceShape(this);
    }

    @Override
    public Optional<ResourceShape> asResourceShape() {
        return Optional.of(this);
    }

    @Override
    public Set<ShapeId> getAllOperations() {
        return Collections.unmodifiableSet(allOperations);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.RESOURCE;
    }

    /**
     * Gets the operations bound through the "collectionOperations" property.
     *
     * This will not include operations bound to resources using a lifecycle
     * operation binding.
     *
     * @return Get the "collectionOperations" directly bound to this shape.
     * @see #getAllOperations()
     */
    public Set<ShapeId> getCollectionOperations() {
        return Collections.unmodifiableSet(collectionOperations);
    }

    /**
     * Gets the identifiers of the resource.
     *
     * @return Returns the identifiers map of name to shape ID.
     */
    public Map<String, ShapeId> getIdentifiers() {
        return identifiers;
    }

    /**
     * @return Returns true if this resource defines any identifiers.
     */
    public boolean hasIdentifiers() {
        return !identifiers.isEmpty();
    }

    public Map<String, ShapeId> getProperties() {
        return properties;
    }

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    /**
     * Gets the put lifecycle operation of the resource.
     *
     * @return Returns the optionally found lifecycle.
     */
    public Optional<ShapeId> getPut() {
        return Optional.ofNullable(put);
    }

    /**
     * Gets the create lifecycle operation of the resource.
     *
     * @return Returns the optionally found lifecycle.
     */
    public Optional<ShapeId> getCreate() {
        return Optional.ofNullable(create);
    }

    /**
     * Gets the read lifecycle operation of the resource.
     *
     * @return Returns the optionally found lifecycle.
     */
    public Optional<ShapeId> getRead() {
        return Optional.ofNullable(read);
    }

    /**
     * Gets the update lifecycle operation of the resource.
     *
     * @return Returns the optionally found lifecycle.
     */
    public Optional<ShapeId> getUpdate() {
        return Optional.ofNullable(update);
    }

    /**
     * Gets the delete lifecycle operation of the resource.
     *
     * @return Returns the optionally found lifecycle.
     */
    public Optional<ShapeId> getDelete() {
        return Optional.ofNullable(delete);
    }

    /**
     * Gets the list lifecycle operation of the resource.
     *
     * @return Returns the optionally found lifecycle.
     */
    public Optional<ShapeId> getList() {
        return Optional.ofNullable(list);
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        ResourceShape otherShape = (ResourceShape) other;
        return identifiers.equals(otherShape.identifiers)
                && Objects.equals(properties, otherShape.properties)
                && Objects.equals(create, otherShape.create)
                && Objects.equals(put, otherShape.put)
                && Objects.equals(read, otherShape.read)
                && Objects.equals(update, otherShape.update)
                && Objects.equals(delete, otherShape.delete)
                && Objects.equals(list, otherShape.list);
    }

    /**
     * Builder used to create a {@link ResourceShape}.
     */
    public static final class Builder extends EntityShape.Builder<Builder, ResourceShape> {
        private final BuilderRef<Map<String, ShapeId>> identifiers = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<String, ShapeId>> properties = BuilderRef.forOrderedMap();
        private final BuilderRef<Set<ShapeId>> collectionOperations = BuilderRef.forOrderedSet();
        private ShapeId put;
        private ShapeId create;
        private ShapeId read;
        private ShapeId update;
        private ShapeId delete;
        private ShapeId list;

        @Override
        public ResourceShape build() {
            return new ResourceShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.RESOURCE;
        }

        /**
         * Sets the resource identifiers map of identifier name to shape ID.
         *
         * @param identifiers The identifiers to set.
         * @return Returns the builder.
         */
        public Builder identifiers(Map<String, ShapeId> identifiers) {
            this.identifiers.clear();
            this.identifiers.get().putAll(identifiers);
            return this;
        }

        /**
         * Adds an identifier to the resource.
         *
         * @param name Name of the identifier.
         * @param identifier Shape ID of the identifier.
         * @return Returns the builder.
         */
        public Builder addIdentifier(String name, ToShapeId identifier) {
            identifiers.get().put(Objects.requireNonNull(name), identifier.toShapeId());
            return this;
        }

        public Builder addIdentifier(String name, String identifier) {
            return addIdentifier(name, ShapeId.from(identifier));
        }

        public Builder properties(Map<String, ShapeId> properties) {
            this.properties.clear();
            this.properties.get().putAll(properties);
            return this;
        }

        public Builder addProperty(String name, ToShapeId targetShape) {
            properties.get().put(Objects.requireNonNull(name), targetShape.toShapeId());
            return this;
        }

        public Builder addProperty(String name, String targetShape) {
            return addProperty(name, ShapeId.from(targetShape));
        }

        public Builder put(ToShapeId put) {
            this.put = put == null ? null : put.toShapeId();
            return this;
        }

        public Builder create(ToShapeId create) {
            this.create = create == null ? null : create.toShapeId();
            return this;
        }

        public Builder read(ToShapeId read) {
            this.read = read == null ? null : read.toShapeId();
            return this;
        }

        public Builder update(ToShapeId update) {
            this.update = update == null ? null : update.toShapeId();
            return this;
        }

        public Builder delete(ToShapeId delete) {
            this.delete = delete == null ? null : delete.toShapeId();
            return this;
        }

        public Builder list(ToShapeId list) {
            this.list = list == null ? null : list.toShapeId();
            return this;
        }

        public Builder collectionOperations(Collection<ShapeId> ids) {
            clearCollectionOperations();
            collectionOperations.get().addAll(ids);
            return this;
        }

        public Builder addCollectionOperation(ToShapeId id) {
            collectionOperations.get().add(id.toShapeId());
            return this;
        }

        public Builder addCollectionOperation(String id) {
            return addCollectionOperation(ShapeId.from(id));
        }

        public Builder removeCollectionOperation(ToShapeId id) {
            collectionOperations.get().remove(id.toShapeId());
            return this;
        }

        public Builder clearCollectionOperations() {
            collectionOperations.clear();
            return this;
        }

        /**
         * Removes an operation binding from lifecycles and the operations list.
         * @param toShapeId Operation ID to remove.
         * @return Returns the builder.
         */
        public Builder removeFromAllOperationBindings(ToShapeId toShapeId) {
            ShapeId id = toShapeId.toShapeId();
            removeOperation(id);
            removeCollectionOperation(id);
            if (Objects.equals(create, id)) {
                create = null;
            }
            if (Objects.equals(put, id)) {
                put = null;
            }
            if (Objects.equals(read, id)) {
                read = null;
            }
            if (Objects.equals(update, id)) {
                update = null;
            }
            if (Objects.equals(delete, id)) {
                delete = null;
            }
            if (Objects.equals(list, id)) {
                list = null;
            }
            return this;
        }
    }
}

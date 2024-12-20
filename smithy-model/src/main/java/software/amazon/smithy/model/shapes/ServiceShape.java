/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code service} shape.
 */
public final class ServiceShape extends EntityShape implements ToSmithyBuilder<ServiceShape> {

    private final String version;
    private final String introducedVersion;
    private final Map<ShapeId, String> rename;
    private final Map<ShapeId, String> introducedRename;
    private final List<ShapeId> errors;
    private final List<ShapeId> introducedErrors;

    private ServiceShape(Builder builder) {
        super(builder);

        if (getMixins().isEmpty()) {
            version = builder.version;
            introducedVersion = version;
            rename = builder.rename.copy();
            introducedRename = rename;
            errors = builder.errors.copy();
            introducedErrors = errors;
        } else {
            String computedVersion = "";
            Map<ShapeId, String> computedRename = new LinkedHashMap<>();
            Set<ShapeId> computedErrors = new LinkedHashSet<>();

            for (Shape shape : builder.getMixins().values()) {
                if (shape.isServiceShape()) {
                    ServiceShape mixin = shape.asServiceShape().get();
                    if (!mixin.version.isEmpty()) {
                        computedVersion = mixin.version;
                    }
                    computedRename.putAll(mixin.getRename());
                    computedErrors.addAll(mixin.getErrors());
                }
            }

            introducedVersion = builder.version;
            introducedRename = builder.rename.copy();
            introducedErrors = builder.errors.copy();

            if (!introducedVersion.isEmpty()) {
                computedVersion = introducedVersion;
            }
            computedRename.putAll(introducedRename);
            computedErrors.addAll(introducedErrors);

            version = computedVersion;
            rename = Collections.unmodifiableMap(computedRename);
            errors = Collections.unmodifiableList(new ArrayList<>(computedErrors));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return updateBuilder(builder())
                .version(introducedVersion)
                .errors(introducedErrors)
                .rename(introducedRename)
                .operations(getIntroducedOperations())
                .resources(getIntroducedResources());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.serviceShape(this);
    }

    @Override
    public Optional<ServiceShape> asServiceShape() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        ServiceShape o = (ServiceShape) other;
        return version.equals(o.version)
                && rename.equals(o.rename)
                && errors.equals(o.errors);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.SERVICE;
    }

    /**
     * Get the version of the service. An empty string is returned
     * if the version is undefined.
     *
     * @return The version of the service.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the version of the service introduced by the shape and not
     * inherited from mixins. An empty string is returned if the version
     * is undefined.
     *
     * @return The introduced version of the service.
     */
    public String getIntroducedVersion() {
        return introducedVersion;
    }

    /**
     * @return The rename map of the service.
     */
    public Map<ShapeId, String> getRename() {
        return rename;
    }

    /**
     * Gets the rename map introduced by the shape and not inherited
     * from mixins.
     *
     * @return The introduced rename map of the service.
     */
    public Map<ShapeId, String> getIntroducedRename() {
        return introducedRename;
    }

    /**
     * <p>Gets a list of the common errors that can be encountered by
     * every operation in the service.</p>
     *
     * <p>Each returned {@link ShapeId} must resolve to a
     * {@link StructureShape} that is targeted by an error trait; however,
     * this is only guaranteed after a model is validated.</p>
     *
     * @return Returns the errors.
     */
    public List<ShapeId> getErrors() {
        return errors;
    }

    /**
     * Gets the list of common errors introduced by the shape and not
     * inherited from mixins. These errors can be encountered by every
     * operation in the service.
     *
     * Each returned {@link ShapeId} must resolve to a
     * {@link StructureShape} that is targeted by an error trait; however,
     * this is only guaranteed after a model is validated.
     *
     * @return Returns the introduced service errors.
     */
    public List<ShapeId> getIntroducedErrors() {
        return introducedErrors;
    }

    /**
     * Gets the contextual name of a shape within the closure.
     *
     * <p>If there is a rename property entry for the given shape ID, then
     * the renamed shape name is returned. Otherwise, the name part of the
     * given shape ID is returned, regardless of if the shape exists in the
     * closure of the service.
     *
     * <p>This is a mirror of {@link ShapeId#getName(ServiceShape)}
     * that serves to make this functionality more discoverable.
     *
     * @param shape Shape to get the contextual name of.
     * @return Returns the contextual name of the shape within the service.
     */
    public String getContextualName(ToShapeId shape) {
        ShapeId id = shape.toShapeId();
        return rename.getOrDefault(id, id.getName());
    }

    /**
     * Builder used to create a {@link ServiceShape}.
     */
    public static final class Builder extends EntityShape.Builder<Builder, ServiceShape> {
        private String version = "";
        private final BuilderRef<Map<ShapeId, String>> rename = BuilderRef.forOrderedMap();
        private final BuilderRef<List<ShapeId>> errors = BuilderRef.forList();

        @Override
        public ServiceShape build() {
            return new ServiceShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.SERVICE;
        }

        public Builder version(String version) {
            this.version = version == null ? "" : version;
            return this;
        }

        public Builder clearRename() {
            this.rename.clear();
            return this;
        }

        public Builder rename(Map<ShapeId, String> rename) {
            clearRename();
            rename.forEach(this::putRename);
            return this;
        }

        public Builder putRename(ShapeId from, String to) {
            this.rename.get().put(Objects.requireNonNull(from), Objects.requireNonNull(to));
            return this;
        }

        public Builder removeRename(ToShapeId from) {
            rename.get().remove(from.toShapeId());
            return this;
        }

        /**
         * Sets and replaces the errors of the service. Each error is implicitly
         * bound to every operation within the closure of the service.
         *
         * @param errorShapeIds Error shape IDs to set.
         * @return Returns the builder.
         */
        public Builder errors(Collection<ShapeId> errorShapeIds) {
            errors.clear();
            errorShapeIds.forEach(this::addError);
            return this;
        }

        /**
         * Adds an error to the service that is implicitly bound to every operation
         * within the closure of the service.
         *
         * @param errorShapeId Error shape ID to add.
         * @return Returns the builder.
         */
        public Builder addError(ToShapeId errorShapeId) {
            errors.get().add(errorShapeId.toShapeId());
            return this;
        }

        /**
         * Adds an error to the service that is implicitly bound to every
         * operation within the closure of the service.
         *
         * @param errorShapeId Error shape ID to add.
         * @return Returns the builder.
         * @throws ShapeIdSyntaxException if the shape ID is invalid.
         */
        public Builder addError(String errorShapeId) {
            return addError(ShapeId.from(errorShapeId));
        }

        /**
         * Adds errors to the service that are implicitly bound to every operation
         * within the closure of the service.
         *
         * @param errorShapeIds Error shape IDs to add.
         * @return Returns the builder.
         */
        public Builder addErrors(List<ShapeId> errorShapeIds) {
            errors.get().addAll(Objects.requireNonNull(errorShapeIds));
            return this;
        }

        /**
         * Removes an error by Shape ID.
         *
         * @param errorShapeId Error shape ID to remove.
         * @return Returns the builder.
         */
        public Builder removeError(ToShapeId errorShapeId) {
            errors.get().remove(errorShapeId.toShapeId());
            return this;
        }

        /**
         * Removes all errors.
         * @return Returns the builder.
         */
        public Builder clearErrors() {
            errors.clear();
            return this;
        }

        @Override
        public Builder flattenMixins() {
            if (getMixins().isEmpty()) {
                return this;
            }

            String flatVersion = version;
            Map<ShapeId, String> flatRename = new LinkedHashMap<>();
            Set<ShapeId> flatErrors = new LinkedHashSet<>();

            for (Shape shape : getMixins().values()) {
                if (shape instanceof ServiceShape) {
                    ServiceShape mixin = (ServiceShape) shape;
                    if (!mixin.version.isEmpty()) {
                        flatVersion = mixin.version;
                    }
                    flatRename.putAll(mixin.getRename());
                    flatErrors.addAll(mixin.getErrors());
                }
            }

            if (!version.isEmpty()) {
                flatVersion = version;
            }
            flatRename.putAll(rename.peek());
            flatErrors.addAll(errors.peek());

            version = flatVersion;
            rename(flatRename);
            errors(flatErrors);
            return super.flattenMixins();
        }
    }
}

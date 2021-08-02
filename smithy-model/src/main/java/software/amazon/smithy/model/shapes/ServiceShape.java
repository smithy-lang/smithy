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

package software.amazon.smithy.model.shapes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code service} shape.
 */
public final class ServiceShape extends EntityShape implements ToSmithyBuilder<ServiceShape> {

    private final String version;
    private final Map<ShapeId, String> rename;
    private final List<ShapeId> errors;

    private ServiceShape(Builder builder) {
        super(builder);
        version = builder.version;
        rename = MapUtils.orderedCopyOf(builder.rename);
        errors = ListUtils.copyOf(builder.errors);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return updateBuilder(builder())
                .version(version)
                .errors(errors)
                .rename(rename)
                .operations(getOperations())
                .resources(getResources());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.serviceShape(this);
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
     * @return The rename map of the service.
     */
    public Map<ShapeId, String> getRename() {
        return rename;
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
        private final Map<ShapeId, String> rename = new TreeMap<>();
        private final List<ShapeId> errors = new ArrayList<>();

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
            this.rename.put(Objects.requireNonNull(from), Objects.requireNonNull(to));
            return this;
        }

        public Builder removeRename(ToShapeId from) {
            rename.remove(from.toShapeId());
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
            errors.add(errorShapeId.toShapeId());
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
            errors.addAll(Objects.requireNonNull(errorShapeIds));
            return this;
        }

        /**
         * Removes an error by Shape ID.
         *
         * @param errorShapeId Error shape ID to remove.
         * @return Returns the builder.
         */
        public Builder removeError(ToShapeId errorShapeId) {
            errors.remove(errorShapeId.toShapeId());
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
    }
}

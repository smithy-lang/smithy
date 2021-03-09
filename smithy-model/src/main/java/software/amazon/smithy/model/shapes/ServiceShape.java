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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code service} shape.
 */
public final class ServiceShape extends EntityShape implements ToSmithyBuilder<ServiceShape> {

    private final String version;
    private final Map<ShapeId, String> rename;

    private ServiceShape(Builder builder) {
        super(builder);
        version = SmithyBuilder.requiredState("version", builder.version);
        rename = MapUtils.orderedCopyOf(builder.rename);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().from(this).version(version);
        getOperations().forEach(builder::addOperation);
        getResources().forEach(builder::addResource);
        builder.rename(rename);
        return builder;
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
        return version.equals(o.version) && rename.equals(o.rename);
    }

    /**
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
        private String version;
        private final Map<ShapeId, String> rename = new TreeMap<>();

        @Override
        public ServiceShape build() {
            return new ServiceShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.SERVICE;
        }

        public Builder version(String version) {
            this.version = version;
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
    }
}

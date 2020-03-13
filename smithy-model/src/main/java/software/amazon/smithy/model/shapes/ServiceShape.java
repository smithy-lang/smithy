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

import java.util.Optional;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code service} shape.
 */
public final class ServiceShape extends EntityShape implements ToSmithyBuilder<ServiceShape> {

    private final String version;

    private ServiceShape(Builder builder) {
        super(builder);
        version = SmithyBuilder.requiredState("version", builder.version);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().from(this).version(version);
        getOperations().forEach(builder::addOperation);
        getResources().forEach(builder::addResource);
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
    public ServiceShape expectServiceShape() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        ServiceShape o = (ServiceShape) other;
        return version.equals(o.version);
    }

    /**
     * @return The version of the service.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Builder used to create a {@link ServiceShape}.
     */
    public static final class Builder extends EntityShape.Builder<Builder, ServiceShape> {
        private String version;

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
    }
}

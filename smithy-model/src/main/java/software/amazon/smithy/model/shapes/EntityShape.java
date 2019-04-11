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

import java.util.HashSet;
import java.util.Set;
import software.amazon.smithy.utils.SetUtils;

/**
 * Abstract class representing service and resource shapes.
 */
public abstract class EntityShape extends Shape {

    private final Set<ShapeId> resources;
    private final Set<ShapeId> operations;

    @SuppressWarnings("unchecked")
    EntityShape(Builder builder, ShapeType type) {
        super(builder, type, false);
        resources = SetUtils.copyOf(builder.resources);
        operations = SetUtils.copyOf(builder.operations);
    }

    /**
     * @return Get all of the resources directly bound to this shape.
     */
    public final Set<ShapeId> getResources() {
        return resources;
    }

    /**
     * @return Get the "operations" directly bound to this shape.
     */
    public Set<ShapeId> getOperations() {
        return operations;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        EntityShape o = (EntityShape) other;
        return resources.equals(o.resources) && operations.equals(o.operations);
    }

    /**
     * Builder used to create a Service or Resource shape.
     * @param <B> Concrete builder type.
     * @param <S> Shape type being created.
     */
    public abstract static class Builder<B extends Builder, S extends EntityShape>
            extends AbstractShapeBuilder<B, S> {

        private final Set<ShapeId> resources = new HashSet<>();
        private final Set<ShapeId> operations = new HashSet<>();

        @SuppressWarnings("unchecked")
        public B addOperation(ToShapeId id) {
            operations.add(id.toShapeId());
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B addOperation(String id) {
            return addOperation(ShapeId.from(id));
        }

        @SuppressWarnings("unchecked")
        public B removeOperation(ToShapeId id) {
            operations.remove(id.toShapeId());
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B clearOperations() {
            operations.clear();
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B addResource(ToShapeId id) {
            resources.add(id.toShapeId());
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B addResource(String id) {
            return addResource(ShapeId.from(id));
        }

        @SuppressWarnings("unchecked")
        public B removeResource(ToShapeId id) {
            resources.remove(id.toShapeId());
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B clearResources() {
            resources.clear();
            return (B) this;
        }
    }
}

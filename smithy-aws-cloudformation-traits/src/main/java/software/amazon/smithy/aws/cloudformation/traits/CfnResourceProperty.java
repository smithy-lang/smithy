/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.aws.cloudformation.traits.CfnResourceIndex.Mutability;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Contains extracted resource property information.
 */
public final class CfnResourceProperty implements ToSmithyBuilder<CfnResourceProperty> {
    private final Set<ShapeId> shapeIds;
    private final Set<Mutability> mutabilities;
    private final boolean hasExplicitMutability;

    private CfnResourceProperty(Builder builder) {
        shapeIds = new TreeSet<>(builder.shapeIds.peek());
        mutabilities = builder.mutabilities.copy();
        hasExplicitMutability = builder.hasExplicitMutability;
    }

    public static Builder builder() {
        return new Builder();
    }

    /*
     * Gets all shape IDs used for this property.
     *
     * <p>The list of potential shape IDs is used only for validation,
     * as having only one shape ID is required.
     */
    Set<ShapeId> getShapeIds() {
        return shapeIds;
    }

    /**
     * Gets the shape ID used to represent this property.
     *
     * @return Returns the shape ID.
     */
    public ShapeId getShapeId() {
        return shapeIds.iterator().next();
    }

    /**
     * Returns true if the property's mutability was configured explicitly
     * by the use of a trait instead of derived through its lifecycle
     * bindings within a resource.
     *
     * <p> Also returns true for identifiers, since their mutability is inherent
     *
     * @return Returns true if the mutability is explicitly defined by a trait or
     * if property is an identifier
     *
     * @see CfnMutabilityTrait
     */
    public boolean hasExplicitMutability() {
        return hasExplicitMutability;
    }

    /**
     * Gets all of the CloudFormation-specific property mutability options
     * associated with this resource property.
     *
     * @return Returns the mutabilities.
     */
    public Set<Mutability> getMutabilities() {
        return mutabilities;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .shapeIds(shapeIds)
                .mutabilities(mutabilities);
    }

    public static final class Builder implements SmithyBuilder<CfnResourceProperty> {
        private final BuilderRef<Set<ShapeId>> shapeIds = BuilderRef.forSortedSet();
        private final BuilderRef<Set<Mutability>> mutabilities = BuilderRef.forUnorderedSet();
        private boolean hasExplicitMutability = false;

        @Override
        public CfnResourceProperty build() {
            return new CfnResourceProperty(this);
        }

        public Builder addShapeId(ShapeId shapeId) {
            this.shapeIds.get().add(shapeId);
            return this;
        }

        public Builder removeShapeId(ShapeId shapeId) {
            this.shapeIds.get().remove(shapeId);
            return this;
        }

        public Builder shapeIds(Set<ShapeId> shapeIds) {
            this.shapeIds.clear();
            shapeIds.forEach(this::addShapeId);
            return this;
        }

        public Builder mutabilities(Set<Mutability> mutabilities) {
            this.mutabilities.clear();
            this.mutabilities.get().addAll(mutabilities);
            return this;
        }

        public Builder hasExplicitMutability(boolean hasExplicitMutability) {
            this.hasExplicitMutability = hasExplicitMutability;
            return this;
        }
    }
}

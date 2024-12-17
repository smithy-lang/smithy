/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Queries a model for effective traits bound to shapes and members.
 */
public final class EffectiveTraitQuery implements ToSmithyBuilder<EffectiveTraitQuery> {

    private final Model model;
    private final Class<? extends Trait> traitClass;
    private final boolean inheritFromContainer;

    private EffectiveTraitQuery(Builder builder) {
        this.model = SmithyBuilder.requiredState("model", builder.model);
        this.traitClass = SmithyBuilder.requiredState("traitClass", builder.traitClass);
        this.inheritFromContainer = builder.inheritFromContainer;
    }

    /**
     * Checks if the trait is effectively applied to a shape.
     *
     * @param shapeId Shape to test.
     * @return Returns true if the trait is effectively applied to the shape.
     */
    public boolean isTraitApplied(ToShapeId shapeId) {
        Shape shape = model.getShape(shapeId.toShapeId()).orElse(null);

        if (shape == null) {
            return false;
        }

        if (shape.getMemberTrait(model, traitClass).isPresent()) {
            return true;
        }

        if (!inheritFromContainer || !shape.asMemberShape().isPresent()) {
            return false;
        }

        // Check if the parent of the member is marked with the trait.
        MemberShape memberShape = shape.asMemberShape().get();
        Shape parent = model.getShape(memberShape.getContainer()).orElse(null);
        return parent != null && parent.hasTrait(traitClass);
    }

    /**
     * Creates a new query builder.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .model(model)
                .traitClass(traitClass)
                .inheritFromContainer(inheritFromContainer);
    }

    /**
     * Builds a reusable EffectiveTraitQuery.
     */
    public static final class Builder implements SmithyBuilder<EffectiveTraitQuery> {

        private Model model;
        private Class<? extends Trait> traitClass;
        private boolean inheritFromContainer;

        @Override
        public EffectiveTraitQuery build() {
            return new EffectiveTraitQuery(this);
        }

        /**
         * Sets the required model to query.
         *
         * @param model Model to query.
         * @return Returns the query object builder.
         */
        public Builder model(Model model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the required trait being queried.
         *
         * @param traitClass Trait to detect on shapes.
         * @return Returns the query object builder.
         */
        public Builder traitClass(Class<? extends Trait> traitClass) {
            this.traitClass = traitClass;
            return this;
        }

        /**
         * When testing member shapes, also checks the container of the member for
         * the presence of a trait.
         *
         * <p>By default, traits are not inherited from a member's parent container.
         *
         * @param inheritFromContainer Set to true to inherit traits from member containers.
         * @return Returns the query object builder.
         */
        public Builder inheritFromContainer(boolean inheritFromContainer) {
            this.inheritFromContainer = inheritFromContainer;
            return this;
        }
    }
}

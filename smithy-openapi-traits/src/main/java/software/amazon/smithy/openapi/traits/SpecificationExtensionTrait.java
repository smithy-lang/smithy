/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.openapi.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * <code>smithy.openapi#specificationExtension</code> - Indicates a trait shape should be converted into an <a href="https://spec.openapis.org/oas/v3.1.0#specification-extensions">OpenAPI specification extension</a>.
 */
public final class SpecificationExtensionTrait extends AbstractTrait
        implements ToSmithyBuilder<SpecificationExtensionTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.openapi#specificationExtension");

    private static final String AS_MEMBER_NAME = "as";

    private final String as;

    private SpecificationExtensionTrait(SpecificationExtensionTrait.Builder builder) {
        super(ID, builder.getSourceLocation());
        this.as = builder.as;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode node = value.expectObjectNode();
            SpecificationExtensionTrait.Builder builder = builder().sourceLocation(value);
            node.getStringMember(AS_MEMBER_NAME, builder::as);
            SpecificationExtensionTrait trait = builder.build();
            trait.setNodeCache(value);
            return trait;
        }
    }

    /**
     * Gets the extension name for a given trait shape.
     * Either an explicitly configured extension name, or a default transformation of the shape ID.
     *
     * @param traitShapeId Trait shape to get the extension name.
     * @return Extension name for the given trait shape.
     */
    public String extensionNameFor(ShapeId traitShapeId) {
        return as != null
                ? as
                : "x-" + traitShapeId.toString().replaceAll("[.#]", "-");
    }

    public static SpecificationExtensionTrait.Builder builder() {
        return new SpecificationExtensionTrait.Builder();
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember(AS_MEMBER_NAME, this.as)
                .build();
    }

    @Override
    public SpecificationExtensionTrait.Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .as(this.as);
    }

    public static final class Builder
            extends AbstractTraitBuilder<SpecificationExtensionTrait, SpecificationExtensionTrait.Builder> {
        private String as;

        private Builder() {
        }

        @Override
        public SpecificationExtensionTrait build() {
            return new SpecificationExtensionTrait(this);
        }

        /**
         * Set the explicit name for the target specification extension.
         *
         * @param as Explicit name for the target specification extension, or null.
         * @return This builder instance.
         */
        public SpecificationExtensionTrait.Builder as(String as) {
            this.as = as;
            return this;
        }
    }
}

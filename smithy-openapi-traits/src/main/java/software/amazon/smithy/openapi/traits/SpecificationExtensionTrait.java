/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.openapi.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
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

    private final String as;

    private SpecificationExtensionTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.as = builder.as;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            SpecificationExtensionTrait trait = new NodeMapper().deserialize(value, SpecificationExtensionTrait.class);
            trait.setNodeCache(value);
            return trait;
        }
    }

    /**
     * Gets the specification extension header value in "as".
     *
     * @return Returns the optionally present "as".
     */
    public Optional<String> getAs() {
        return Optional.ofNullable(as);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Node createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(SpecificationExtensionTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .as(this.as);
    }

    /**
     * Builds a {@link SpecificationExtensionTrait} trait.
     */
    public static final class Builder extends AbstractTraitBuilder<SpecificationExtensionTrait, Builder> {
        private String as;

        private Builder() {}

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
        public Builder as(String as) {
            this.as = as;
            return this;
        }
    }
}

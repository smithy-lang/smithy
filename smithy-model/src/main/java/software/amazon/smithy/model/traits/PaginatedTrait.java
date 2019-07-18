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

package software.amazon.smithy.model.traits;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.validators.PaginatedTraitValidator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the pagination functionality of an operation.
 *
 * @see PaginatedTraitValidator
 */
public final class PaginatedTrait extends AbstractTrait implements ToSmithyBuilder<PaginatedTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#paginated");

    private static final List<String> PAGINATED_PROPERTIES = ListUtils.of(
            "items", "inputToken", "outputToken", "pageSize");

    private final String items;
    private final String inputToken;
    private final String outputToken;
    private final String pageSize;

    private PaginatedTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        inputToken = builder.inputToken;
        outputToken = builder.outputToken;
        items = builder.items;
        pageSize = builder.pageSize;
    }

    /**
     * @return The `items` property of the trait.
     */
    public Optional<String> getItems() {
        return Optional.ofNullable(items);
    }

    /**
     * @return The `inputToken` property of the trait.
     */
    public Optional<String> getInputToken() {
        return Optional.ofNullable(inputToken);
    }

    /**
     * @return The `outputToken` property of the trait.
     */
    public Optional<String> getOutputToken() {
        return Optional.ofNullable(outputToken);
    }

    /**
     * @return The optional `pageSize` property.
     */
    public Optional<String> getPageSize() {
        return Optional.ofNullable(pageSize);
    }

    /**
     * Merges this paginated trait with another trait.
     *
     * <p>Values set on this trait take precedence over values of the
     * other trait. This operation is typically performed to merge the
     * paginated trait of an operation with the paginated trait of a service.
     *
     * <p>If {@code other} is null, the current trait is returned as-is.
     *
     * @param other Other trait to merge with.
     * @return Returns a newly created trait that maintains the same source location.
     */
    public PaginatedTrait merge(PaginatedTrait other) {
        if (other == null) {
            return this;
        }

        return builder()
                .inputToken(inputToken != null ? inputToken : other.inputToken)
                .outputToken(outputToken != null ? outputToken : other.outputToken)
                .pageSize(pageSize != null ? pageSize : other.pageSize)
                .items(items != null ? items : other.items)
                .sourceLocation(getSourceLocation())
                .build();
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("inputToken", getInputToken().map(Node::from))
                .withOptionalMember("outputToken", getOutputToken().map(Node::from))
                .withOptionalMember("items", getItems().map(Node::from))
                .withOptionalMember("pageSize", getPageSize().map(Node::from));
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .items(items)
                .inputToken(inputToken)
                .outputToken(outputToken)
                .pageSize(pageSize);
    }

    /**
     * @return Returns a paginated trait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds the paginated trait.
     */
    public static final class Builder extends AbstractTraitBuilder<PaginatedTrait, Builder> {
        private String items;
        private String inputToken;
        private String outputToken;
        private String pageSize;

        public Builder items(String memberName) {
            items = memberName;
            return this;
        }

        public Builder inputToken(String memberName) {
            inputToken = memberName;
            return this;
        }

        public Builder outputToken(String memberName) {
            outputToken = memberName;
            return this;
        }

        public Builder pageSize(String memberName) {
            pageSize = memberName;
            return this;
        }

        @Override
        public PaginatedTrait build() {
            return new PaginatedTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public PaginatedTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ObjectNode members = value.expectObjectNode();
            members.warnIfAdditionalProperties(PAGINATED_PROPERTIES);
            builder.pageSize(members.getStringMemberOrDefault("pageSize", null));
            builder.items(members.getStringMemberOrDefault("items", null));
            builder.inputToken(members.getStringMemberOrDefault("inputToken", null));
            builder.outputToken(members.getStringMemberOrDefault("outputToken", null));
            return builder.build();
        }
    }
}

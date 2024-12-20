/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
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
    private static final Pattern PATH_PATTERN = Pattern.compile("\\.");

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
     * Resolves an output path.
     *
     * <p>A path is a series of identifiers separated by dots (`.`) where each identifier
     * represents a member name in a structure.
     *
     * @param path  The path to resolve.
     * @param model The model to be searched when resolving the path.
     * @param shape The shape where path resolution should start, e.g. the output shape
     *              of an operation.
     * @return The optional member shape that the path resolves to.
     * @deprecated This method only returns the last {@link MemberShape} of an output path. To resolve each path
     * identifier to it's respective {@link MemberShape} see {@link PaginatedTrait#resolveFullPath}
     */
    @Deprecated
    public static Optional<MemberShape> resolvePath(
            String path,
            Model model,
            StructureShape shape
    ) {
        List<MemberShape> memberShapes = resolveFullPath(path, model, shape);
        if (memberShapes.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(memberShapes.get(memberShapes.size() - 1));
    }

    /**
     * Resolves an output path to a list of {@link MemberShape}.
     *
     * <p>A path is a series of identifiers separated by dots (`.`) where each identifier
     * represents a member name in a structure.
     *
     * @param path  The path to resolve.
     * @param model The model to be searched when resolving the path.
     * @param shape The shape where path resolution should start, e.g. the output shape
     *              of an operation.
     * @return The list of member shapes corresponding to each path identifier. An unresolvable path will be returned
     * as an empty list.
     */
    public static List<MemberShape> resolveFullPath(
            String path,
            Model model,
            StructureShape shape
    ) {
        List<MemberShape> memberShapes = new ArrayList<>();

        // For each member name in the path, try to find that member in the previous structure
        Optional<MemberShape> memberShape;
        Optional<StructureShape> container = Optional.of(shape);
        for (String memberName : PATH_PATTERN.split(path)) {
            memberShape = container.flatMap(structure -> structure.getMember(memberName));
            if (!memberShape.isPresent()) {
                return ListUtils.of();
            }
            memberShapes.add(memberShape.get());
            container = model.getShape(memberShape.get().getTarget())
                    .flatMap(Shape::asStructureShape);
        }
        return memberShapes;
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
            value.expectObjectNode()
                    .getStringMember("pageSize", builder::pageSize)
                    .getStringMember("items", builder::items)
                    .getStringMember("inputToken", builder::inputToken)
                    .getStringMember("outputToken", builder::outputToken);
            PaginatedTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}

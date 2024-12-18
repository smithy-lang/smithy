/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.smoketests.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.Tagged;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines a single smoke test case.
 */
public final class SmokeTestCase implements Tagged, ToNode, ToSmithyBuilder<SmokeTestCase> {
    private static final String ID = "id";
    private static final String PARAMS = "params";
    private static final String VENDOR_PARAMS = "vendorParams";
    private static final String VENDOR_PARAMS_SHAPE = "vendorParamsShape";
    private static final String EXPECT = "expect";
    private static final String TAGS = "tags";

    private final String id;
    private final ObjectNode params;
    private final ObjectNode vendorParams;
    private final ShapeId vendorParamsShape;
    private final Expectation expectation;
    private final List<String> tags;

    private SmokeTestCase(Builder builder) {
        this.id = SmithyBuilder.requiredState(ID, builder.id);
        this.params = builder.params;
        this.vendorParams = builder.vendorParams;
        this.vendorParamsShape = builder.vendorParamsShape;
        this.expectation = SmithyBuilder.requiredState(EXPECT, builder.expectation);
        this.tags = ListUtils.copyOf(builder.tags);
    }

    /**
     * @return Creates a builder used to build a {@link SmokeTestCase}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a {@link SmokeTestCase} from a {@link Node}.
     *
     * @param node Node to deserialize into a {@link SmokeTestCase}.
     * @return Returns the created {@link SmokeTestCase}.
     */
    public static SmokeTestCase fromNode(Node node) {
        Builder builder = builder();
        ObjectNode o = node.expectObjectNode();
        builder.id(o.expectStringMember(ID).getValue());
        o.getObjectMember(PARAMS).ifPresent(builder::params);
        o.getObjectMember(VENDOR_PARAMS).ifPresent(builder::vendorParams);
        o.getStringMember(VENDOR_PARAMS_SHAPE).map(StringNode::expectShapeId).ifPresent(builder::vendorParamsShape);
        builder.expectation(Expectation.fromNode(o.expectObjectMember(EXPECT)));
        o.getArrayMember(TAGS).ifPresent(tags -> builder.tags(tags.getElementsAs(StringNode::getValue)));
        return builder.build();
    }

    /**
     * Get the ID of this test case.
     *
     * <p>This ID is unique across all smoke test cases for all operations
     * bound to the same service, and can be used to generate test case names.
     *
     * @return Returns the smoke test case ID.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Get the input parameters of this test case.
     *
     * <p>These parameters, if present, are passed to the operation the smoke test
     * targets. If not present, the operation does not have any input.
     *
     * @return Returns the optionally present input parameters.
     */
    public Optional<ObjectNode> getParams() {
        return Optional.ofNullable(this.params);
    }

    /**
     * Get the vendor-specific parameters of this test case.
     *
     * <p>These are custom parameters that can be used to influence the
     * request for this test case.
     *
     * <p>These parameters match the shape id returned by {@link #getVendorParamsShape()},
     * if present.
     *
     * @see #getVendorParamsShape()
     * @return Returns the optionally present vendor-specific parameters.
     */
    public Optional<ObjectNode> getVendorParams() {
        return Optional.ofNullable(this.vendorParams);
    }

    /**
     * Get the shape ID of vendor-specific parameters used by this test case.
     *
     * <p>If present, {@link #getVendorParams()} will match this shape's definition.
     *
     * @see #getVendorParams()
     * @return The optionally present shape ID of vendor-specific parameters.
     */
    public Optional<ShapeId> getVendorParamsShape() {
        return Optional.ofNullable(this.vendorParamsShape);
    }

    /**
     * Get the expected response from the service call for this test case.
     *
     * <p>This expectation can be either a successful response, any error
     * response, or a specific error response.
     *
     * @return Returns expectation.
     */
    public Expectation getExpectation() {
        return this.expectation;
    }

    @Override
    public List<String> getTags() {
        return this.tags;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .id(this.getId())
                .params(this.getParams().orElse(null))
                .vendorParams(this.getVendorParams().orElse(null))
                .vendorParamsShape(this.getVendorParamsShape().orElse(null))
                .expectation(this.getExpectation())
                .tags(this.getTags());
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember(ID, this.getId())
                .withOptionalMember(PARAMS, this.getParams())
                .withOptionalMember(VENDOR_PARAMS, this.getVendorParams())
                .withOptionalMember(VENDOR_PARAMS_SHAPE,
                        this.getVendorParamsShape().map(ShapeId::toString).map(Node::from))
                .withMember(EXPECT, this.getExpectation());

        if (!this.tags.isEmpty()) {
            builder.withMember(TAGS, ArrayNode.fromStrings(tags));
        }

        return builder.build();

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || o.getClass() != getClass()) {
            return false;
        } else {
            return toNode().equals(((SmokeTestCase) o).toNode());
        }
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }

    public static final class Builder implements SmithyBuilder<SmokeTestCase> {
        private String id;
        private ObjectNode params;
        private ObjectNode vendorParams;
        private ShapeId vendorParamsShape;
        private Expectation expectation;
        private final List<String> tags = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the smoke test case ID.
         *
         * <p>Must be unique across all smoke test cases on other operations
         * bound to the same service.
         *
         * <p>Must match the following regex: {@code ^[A-Za-z_][A-Za-z0-9_]+$}
         *
         * @param id Test case ID.
         * @return Returns the builder.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the input parameters to the operation targeted by the smoke test case.
         *
         * <p>These parameters must be compatible with the operation's input.
         *
         * @param params Test case operation input parameters.
         * @return Returns the builder.
         */
        public Builder params(ObjectNode params) {
            this.params = params;
            return this;
        }

        /**
         * Sets vendor-specific parameters for the test case which can be used to
         * influence the request.
         *
         * <p>If {@link #vendorParamsShape(ShapeId)} is set, these parameters must
         * match that shape's definition.
         *
         * @param vendorParams Vendor-specific parameters for the test case.
         * @return Returns the builder.
         */
        public Builder vendorParams(ObjectNode vendorParams) {
            this.vendorParams = vendorParams;
            return this;
        }

        /**
         * Sets the shape ID of vendor-specific parameters for the test case.
         *
         * <p>If set, {@link #vendorParams(ObjectNode)} must be compatible with
         * this shape's definition
         *
         * @param vendorParamsShape Shape ID of vendor-specific parameters.
         * @return Returns the builder.
         */
        public Builder vendorParamsShape(ShapeId vendorParamsShape) {
            this.vendorParamsShape = vendorParamsShape;
            return this;
        }

        /**
         * Sets the expected response of the service call for the test case.
         *
         * @param expectation Expected response of the service call.
         * @return Returns the builder.
         */
        public Builder expectation(Expectation expectation) {
            this.expectation = expectation;
            return this;
        }

        /**
         * Sets the tags that can be used to categorize and group test cases.
         *
         * @param tags Tags to attach to the test case.
         * @return Returns the builder.
         */
        public Builder tags(List<String> tags) {
            this.tags.clear();
            this.tags.addAll(tags);
            return this;
        }

        @Override
        public SmokeTestCase build() {
            return new SmokeTestCase(this);
        }
    }
}

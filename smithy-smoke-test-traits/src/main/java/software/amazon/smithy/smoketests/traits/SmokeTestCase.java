/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

    public String getId() {
        return this.id;
    }

    public Optional<ObjectNode> getParams() {
        return Optional.ofNullable(this.params);
    }

    public Optional<ObjectNode> getVendorParams() {
        return Optional.ofNullable(this.vendorParams);
    }

    public Optional<ShapeId> getVendorParamsShape() {
        return Optional.ofNullable(this.vendorParamsShape);
    }

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

    public static Builder builder() {
        return new Builder();
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

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder params(ObjectNode params) {
            this.params = params;
            return this;
        }

        public Builder vendorParams(ObjectNode vendorParams) {
            this.vendorParams = vendorParams;
            return this;
        }

        public Builder vendorParamsShape(ShapeId vendorParamsShape) {
            this.vendorParamsShape = vendorParamsShape;
            return this;
        }

        public Builder expectation(Expectation expectation) {
            this.expectation = expectation;
            return this;
        }

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

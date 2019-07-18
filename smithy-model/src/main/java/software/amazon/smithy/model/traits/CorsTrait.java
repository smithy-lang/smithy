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

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class CorsTrait extends AbstractTrait implements ToSmithyBuilder<CorsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#cors");

    private static final String DEFAULT_ORIGIN = "*";
    private static final int DEFAULT_MAX_AGE = 600;
    private static final String ORIGIN_MEMBER_ID = "origin";
    private static final String MAX_AGE_MEMBER_ID = "maxAge";
    private static final String ALLOWED_HEADERS_MEMBER_ID = "additionalAllowedHeaders";
    private static final String EXPOSED_HEADERS_MEMBER_ID = "additionalExposedHeaders";

    private final String origin;
    private final int maxAge;
    private final Set<String> additionalAllowedHeaders;
    private final Set<String> additionalExposedHeaders;

    private CorsTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        origin = builder.origin;
        maxAge = builder.maxAge;
        additionalAllowedHeaders = SetUtils.copyOf(builder.additionalAllowedHeaders);
        additionalExposedHeaders = SetUtils.copyOf(builder.additionalExposedHeaders);
    }

    public String getOrigin() {
        return origin;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public Set<String> getAdditionalAllowedHeaders() {
        return additionalAllowedHeaders;
    }

    public Set<String> getAdditionalExposedHeaders() {
        return additionalExposedHeaders;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .origin(origin)
                .maxAge(maxAge)
                .additionalAllowedHeaders(additionalAllowedHeaders)
                .additionalExposedHeaders(additionalExposedHeaders);
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember(ORIGIN_MEMBER_ID, Optional.of(origin)
                        .filter(val -> !val.equals(DEFAULT_ORIGIN))
                        .map(Node::from))
                .withOptionalMember(MAX_AGE_MEMBER_ID, Optional.of(maxAge)
                        .filter(val -> !val.equals(DEFAULT_MAX_AGE))
                        .map(Node::from))
                .withOptionalMember(ALLOWED_HEADERS_MEMBER_ID, Optional.of(additionalAllowedHeaders)
                        .filter(FunctionalUtils.not(Set::isEmpty))
                        .map(Node::fromStrings))
                .withOptionalMember(EXPOSED_HEADERS_MEMBER_ID, Optional.of(additionalExposedHeaders)
                        .filter(FunctionalUtils.not(Set::isEmpty))
                        .map(Node::fromStrings));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractTraitBuilder<CorsTrait, Builder> {

        private String origin = DEFAULT_ORIGIN;
        private int maxAge = DEFAULT_MAX_AGE;
        private Set<String> additionalAllowedHeaders = SetUtils.of();
        private Set<String> additionalExposedHeaders = SetUtils.of();

        private Builder() {}

        public Builder origin(String origin) {
            this.origin = Objects.requireNonNull(origin);
            return this;
        }

        public Builder maxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder additionalAllowedHeaders(Set<String> additionalAllowedHeaders) {
            this.additionalAllowedHeaders = new HashSet<>(Objects.requireNonNull(additionalAllowedHeaders));
            return this;
        }

        public Builder additionalExposedHeaders(Set<String> additionalExposedHeaders) {
            this.additionalExposedHeaders = new HashSet<>(Objects.requireNonNull(additionalExposedHeaders));
            return this;
        }

        @Override
        public CorsTrait build() {
            return new CorsTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public CorsTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ObjectNode node = value.expectObjectNode();
            node.getStringMember(ORIGIN_MEMBER_ID)
                    .map(StringNode::getValue)
                    .ifPresent(builder::origin);
            node.getNumberMember(MAX_AGE_MEMBER_ID)
                    .map(NumberNode::getValue)
                    .map(Number::intValue)
                    .ifPresent(builder::maxAge);
            node.getArrayMember(ALLOWED_HEADERS_MEMBER_ID)
                    .map(Provider::stringSetFromNode)
                    .ifPresent(builder::additionalAllowedHeaders);
            node.getArrayMember(EXPOSED_HEADERS_MEMBER_ID)
                    .map(Provider::stringSetFromNode)
                    .ifPresent(builder::additionalExposedHeaders);
            return builder.build();
        }

        private static Set<String> stringSetFromNode(ArrayNode node) {
            return node.getElements()
                    .stream()
                    .map(Node::expectStringNode)
                    .map(StringNode::getValue)
                    .collect(Collectors.toSet());
        }
    }
}

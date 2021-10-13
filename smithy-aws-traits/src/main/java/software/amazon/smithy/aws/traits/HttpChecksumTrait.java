/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that an operation supports checksum validation.
 * Defines the checksum behavior for operations HTTP Request and HTTP Response.
 */
@SmithyUnstableApi
public final class HttpChecksumTrait extends AbstractTrait implements ToSmithyBuilder<HttpChecksumTrait> {
    public static final ShapeId ID = ShapeId.from("aws.protocols#httpChecksum");

    private static final String REQUEST_CHECKSUM_REQUIRED = "requestChecksumRequired";
    private static final String REQUEST_ALGORITHM_MEMBER = "requestAlgorithmMember";
    private static final String REQUEST_VALIDATION_MODE_MEMBER = "requestValidationModeMember";
    private static final String RESPONSE_ALGORITHMS = "responseAlgorithms";

    private final boolean requestChecksumRequired;
    private final String requestAlgorithmMember;
    private final String requestValidationModeMember;
    private final List<String> responseAlgorithms;

    private HttpChecksumTrait(HttpChecksumTrait.Builder builder) {
        super(ID, builder.getSourceLocation());
        this.requestChecksumRequired = builder.requestChecksumRequired;
        this.requestAlgorithmMember = builder.requestAlgorithmMember;
        this.requestValidationModeMember = builder.requestValidationModeMember;
        this.responseAlgorithms = ListUtils.copyOf(builder.responseAlgorithms);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SmithyBuilder<HttpChecksumTrait> toBuilder() {
        return new Builder()
                .sourceLocation(getSourceLocation())
                .requestChecksumRequired(requestChecksumRequired)
                .requestAlgorithmMember(requestAlgorithmMember)
                .requestValidationModeMember(requestValidationModeMember)
                .responseAlgorithms(responseAlgorithms);
    }

    /**
     * Returns true if request requires checksum to be present.
     *
     * @return Returns if checksum is required.
     */
    public boolean isRequestChecksumRequired() {
        return requestChecksumRequired;
    }

    /**
     * Optionally gets name of the input member used to specify which request checksum to send.
     *
     * @return Returns optional input member name.
     */
    public Optional<String> getRequestAlgorithmMember() {
        return Optional.ofNullable(requestAlgorithmMember);
    }

    /**
     * Gets list of checksum algorithms for which checksum values when present in HTTP response should be validated.
     *
     * @return Returns checksum properties for response.
     */
    public List<String> getResponseAlgorithms() {
        return responseAlgorithms;
    }

    /**
     * Optionally gets name of the input member used to specify opt-in behavior
     * for response checksum validation.
     *
     * @return Returns optional input member name.
     */
    public Optional<String> getRequestValidationModeMember() {
        return Optional.ofNullable(requestValidationModeMember);
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withOptionalMember(REQUEST_ALGORITHM_MEMBER, getRequestAlgorithmMember().map(Node::from))
                .withOptionalMember(REQUEST_VALIDATION_MODE_MEMBER, getRequestValidationModeMember().map(Node::from));

        if (isRequestChecksumRequired()) {
            builder.withMember(REQUEST_CHECKSUM_REQUIRED, Node.from(isRequestChecksumRequired()));
        }

        if (!getResponseAlgorithms().isEmpty()) {
            builder.withMember(RESPONSE_ALGORITHMS, getResponseAlgorithms().stream().map(Node::from)
                    .collect(ArrayNode.collect()));
        }

        return builder.build();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);
            builder.requestChecksumRequired(objectNode.getBooleanMemberOrDefault(REQUEST_CHECKSUM_REQUIRED));

            Optional<StringNode> requestAlgorithmNode = objectNode.getStringMember(REQUEST_ALGORITHM_MEMBER);
            if (requestAlgorithmNode.isPresent()) {
                builder.requestAlgorithmMember(requestAlgorithmNode.get().getValue());
            }

            Optional<ArrayNode> responseAlgorithmNodes = objectNode.getArrayMember(RESPONSE_ALGORITHMS);
            if (responseAlgorithmNodes.isPresent()) {
                List<String> algorithms = responseAlgorithmNodes.get()
                        .getElementsAs(StringNode::getValue);
                for (String algorithm : algorithms) {
                    builder.addResponseAlgorithm(algorithm);
                }
            }

            Optional<StringNode> requestValidationModeMemberNode = objectNode.getStringMember(
                    REQUEST_VALIDATION_MODE_MEMBER);
            if (requestValidationModeMemberNode.isPresent()) {
                builder.requestValidationModeMember(requestValidationModeMemberNode.get().getValue());
            }

            return builder.build();
        }
    }

    public static final class Builder extends AbstractTraitBuilder<HttpChecksumTrait, Builder> {
        private String requestAlgorithmMember;
        private boolean requestChecksumRequired;

        private String requestValidationModeMember;
        private List<String> responseAlgorithms = new ArrayList<>();

        private Builder() {}

        @Override
        public HttpChecksumTrait build() {
            return new HttpChecksumTrait(this);
        }

        public Builder requestAlgorithmMember(String input) {
            this.requestAlgorithmMember = input;
            return this;
        }

        public Builder requestChecksumRequired(boolean isRequired) {
            this.requestChecksumRequired = isRequired;
            return this;
        }

        public Builder requestValidationModeMember(String input) {
            this.requestValidationModeMember = input;
            return this;
        }

        public Builder responseAlgorithms(List<String> algorithms) {
            clearResponseAlgorithms();
            algorithms.forEach(this::addResponseAlgorithm);
            return this;
        }

        public Builder addResponseAlgorithm(String algorithm) {
            this.responseAlgorithms.add(algorithm);
            return this;
        }

        public Builder clearResponseAlgorithms() {
            this.responseAlgorithms.clear();
            return this;
        }
    }
}

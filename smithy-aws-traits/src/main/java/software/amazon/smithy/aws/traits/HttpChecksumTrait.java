/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that an operation supports checksum validation.
 */
@SmithyUnstableApi
public final class HttpChecksumTrait extends AbstractTrait implements ToSmithyBuilder<HttpChecksumTrait> {
    public static final ShapeId ID = ShapeId.from("aws.protocols#httpChecksum");
    public static final String CHECKSUM_PREFIX = "x-amz-checksum-";
    // This list should be in sync with the trait definition in `aws.protocols.smithy`.
    public static final List<String> CHECKSUM_ALGORITHMS = ListUtils.of("CRC64NVME",
            "CRC32C",
            "CRC32",
            "SHA1",
            "SHA256");
    public static final List<String> VALIDATION_MODES = ListUtils.of("ENABLED");

    public static final String REQUEST_CHECKSUM_REQUIRED = "requestChecksumRequired";
    public static final String REQUEST_ALGORITHM_MEMBER = "requestAlgorithmMember";
    public static final String REQUEST_VALIDATION_MODE_MEMBER = "requestValidationModeMember";
    public static final String RESPONSE_ALGORITHMS = "responseAlgorithms";

    private final boolean requestChecksumRequired;
    private final String requestAlgorithmMember;
    private final String requestValidationModeMember;
    private final List<String> responseAlgorithms;

    private HttpChecksumTrait(HttpChecksumTrait.Builder builder) {
        super(ID, builder.getSourceLocation());
        this.requestChecksumRequired = builder.requestChecksumRequired;
        this.requestAlgorithmMember = builder.requestAlgorithmMember;
        this.requestValidationModeMember = builder.requestValidationModeMember;
        this.responseAlgorithms = builder.responseAlgorithms.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
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

    /**
     * Gets the normalized location name for a checksum algorithm.
     *
     * @param checksumAlgorithm The algorithm to get a location name of.
     * @return The normalized location name.
     */
    public static String getChecksumLocationName(String checksumAlgorithm) {
        return CHECKSUM_PREFIX + checksumAlgorithm.toLowerCase(Locale.US);
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withOptionalMember(REQUEST_ALGORITHM_MEMBER, getRequestAlgorithmMember().map(Node::from))
                .withOptionalMember(REQUEST_VALIDATION_MODE_MEMBER, getRequestValidationModeMember().map(Node::from));

        if (isRequestChecksumRequired()) {
            builder.withMember(REQUEST_CHECKSUM_REQUIRED, Node.from(true));
        }

        if (!getResponseAlgorithms().isEmpty()) {
            builder.withMember(RESPONSE_ALGORITHMS, Node.fromStrings(getResponseAlgorithms()));
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

            objectNode.getStringMember(REQUEST_ALGORITHM_MEMBER)
                    .map(StringNode::getValue)
                    .ifPresent(builder::requestAlgorithmMember);

            objectNode.getArrayMember(RESPONSE_ALGORITHMS).ifPresent(responseAlgorithmNodes -> {
                for (String algorithm : responseAlgorithmNodes.getElementsAs(StringNode::getValue)) {
                    builder.addResponseAlgorithm(algorithm);
                }
            });

            objectNode.getStringMember(REQUEST_VALIDATION_MODE_MEMBER)
                    .map(StringNode::getValue)
                    .ifPresent(builder::requestValidationModeMember);

            HttpChecksumTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<HttpChecksumTrait, Builder> {
        private String requestAlgorithmMember;
        private boolean requestChecksumRequired;

        private String requestValidationModeMember;
        private final BuilderRef<List<String>> responseAlgorithms = BuilderRef.forList();

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
            this.responseAlgorithms.clear();
            this.responseAlgorithms.get().addAll(algorithms);
            return this;
        }

        public Builder addResponseAlgorithm(String algorithm) {
            this.responseAlgorithms.get().add(algorithm);
            return this;
        }

        public Builder clearResponseAlgorithms() {
            this.responseAlgorithms.clear();
            return this;
        }
    }
}

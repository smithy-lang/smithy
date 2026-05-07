/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * TLS configuration for an API Gateway integration.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-extensions-integration-tls-config.html">TLS config</a>
 */
public final class TlsConfig implements ToNode, ToSmithyBuilder<TlsConfig> {

    private final Boolean insecureSkipVerification;
    private final FromSourceLocation sourceLocation;

    private TlsConfig(Builder builder) {
        insecureSkipVerification = builder.insecureSkipVerification;
        sourceLocation = builder.sourceLocation;
    }

    /**
     * Creates a builder used to build a TlsConfig.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets whether API Gateway skips verification that the certificate for
     * an integration endpoint is issued by a supported certificate authority.
     *
     * <p>Supported only for HTTP and HTTP_PROXY integrations.
     *
     * @return Returns the optional skip-verification flag.
     */
    public Optional<Boolean> getInsecureSkipVerification() {
        return Optional.ofNullable(insecureSkipVerification);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(sourceLocation)
                .insecureSkipVerification(insecureSkipVerification);
    }

    @Override
    public Node toNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(TlsConfig.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof TlsConfig)) {
            return false;
        }
        return toNode().equals(((TlsConfig) o).toNode());
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }

    /**
     * Builds a {@link TlsConfig}.
     */
    public static final class Builder implements SmithyBuilder<TlsConfig> {
        private Boolean insecureSkipVerification;
        private FromSourceLocation sourceLocation;

        @Override
        public TlsConfig build() {
            return new TlsConfig(this);
        }

        /**
         * Sets whether API Gateway skips certificate verification.
         *
         * @param insecureSkipVerification True to skip verification.
         * @return Returns the builder.
         */
        public Builder insecureSkipVerification(Boolean insecureSkipVerification) {
            this.insecureSkipVerification = insecureSkipVerification;
            return this;
        }

        Builder sourceLocation(FromSourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }
    }
}

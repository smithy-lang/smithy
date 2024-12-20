/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents an API Gateway authorizer.
 *
 * @see AuthorizersTrait
 */
public final class AuthorizerDefinition implements ToNode, ToSmithyBuilder<AuthorizerDefinition> {
    private static final String SIGV4_AUTH_TYPE = "awsSigv4";
    private static final String SCHEME_KEY = "scheme";

    private final ShapeId scheme;
    private final String type;
    private final String customAuthType;
    private final String uri;
    private final String credentials;
    private final String identitySource;
    private final String identityValidationExpression;
    private final Integer resultTtlInSeconds;
    private final String authorizerPayloadFormatVersion;
    private final Boolean enableSimpleResponses;

    private AuthorizerDefinition(Builder builder) {
        scheme = SmithyBuilder.requiredState(SCHEME_KEY, builder.scheme);
        type = builder.type;
        uri = builder.uri;
        credentials = builder.credentials;
        identitySource = builder.identitySource;
        identityValidationExpression = builder.identityValidationExpression;
        resultTtlInSeconds = builder.resultTtlInSeconds;
        authorizerPayloadFormatVersion = builder.authorizerPayloadFormatVersion;
        enableSimpleResponses = builder.enableSimpleResponses;

        if (builder.customAuthType != null) {
            customAuthType = builder.customAuthType;
        } else if (scheme.equals(SigV4Trait.ID)) {
            customAuthType = SIGV4_AUTH_TYPE;
        } else {
            customAuthType = null;
        }
    }

    /**
     * Creates a builder for an Authorizer.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the Smithy scheme used as the client authentication type.
     *
     * @return Returns the defined client authentication type.
     */
    public ShapeId getScheme() {
        return scheme;
    }

    /**
     * Gets the type of the authorizer.
     *
     * <p>If specifying information beyond the scheme, this value is
     * required. The value must be "token", for an authorizer with the
     * caller identity embedded in an authorization token, or "request",
     * for an authorizer with the caller identity contained in request
     * parameters.
     *
     * @return Returns the optional authorizer type.
     */
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    /**
     * Gets the Uniform Resource Identifier (URI) of the authorizer
     * Lambda function.
     *
     * @return Returns the optional Lambda URI.
     */
    public Optional<String> getUri() {
        return Optional.ofNullable(uri);
    }

    /**
     * Gets the customAuthType of the authorizer.
     *
     * <p>This value is not used directly by APIGateway but will be used for
     * OpenAPI exports. This will default to "awsSigV4" if your scheme is
     * {@code aws.auth#sigv4}.
     *
     * @return Returns the customAuthType.
     */
    public Optional<String> getCustomAuthType() {
        return Optional.ofNullable(customAuthType);
    }

    /**
     * Gets the Credentials required for invoking the authorizer, if any, in
     * the form of an ARN of an IAM execution role.
     *
     * <p>For example, "arn:aws:iam::account-id:IAM_role".
     *
     * @return Returns the optional credential ARN.
     */
    public Optional<String> getCredentials() {
        return Optional.ofNullable(credentials);
    }

    /**
     * Gets the comma-separated list of mapping expressions of the request
     * parameters as the identity source.
     *
     * <p>This property is only applicable for the authorizer of the
     * "request" type only.
     *
     * @return Returns the optional identity source string.
     */
    public Optional<String> getIdentitySource() {
        return Optional.ofNullable(identitySource);
    }

    /**
     * Gets the regular expression for validating the token as the incoming
     * identity. For example, {@code "^x-[a-z]+"}.
     *
     * @return Returns the identity regular expression.
     */
    public Optional<String> getIdentityValidationExpression() {
        return Optional.ofNullable(identityValidationExpression);
    }

    /**
     * Gets the number of seconds during which the resulting IAM policy
     * is cached.
     *
     * @return Returns the cache amount in seconds.
     */
    public Optional<Integer> getResultTtlInSeconds() {
        return Optional.ofNullable(resultTtlInSeconds);
    }

    /**
     * Gets the format of the payload returned by the authorizer.
     *
     * @return Returns payload type.
     */
    public Optional<String> getAuthorizerPayloadFormatVersion() {
        return Optional.ofNullable(authorizerPayloadFormatVersion);
    }

    /**
     * Gets whether the authorizer returns simple responses.
     *
     * @return Returns true if authorizer returns a boolean,
     * false if it returns an IAM policy.
     */
    public Optional<Boolean> getEnableSimpleResponses() {
        return Optional.ofNullable(enableSimpleResponses);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .scheme(scheme)
                .type(type)
                .uri(uri)
                .customAuthType(customAuthType)
                .credentials(credentials)
                .identitySource(identitySource)
                .identityValidationExpression(identityValidationExpression)
                .resultTtlInSeconds(resultTtlInSeconds)
                .authorizerPayloadFormatVersion(authorizerPayloadFormatVersion)
                .enableSimpleResponses(enableSimpleResponses);
    }

    @Override
    public Node toNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(AuthorizerDefinition.class);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof AuthorizerDefinition)) {
            return false;
        }

        AuthorizerDefinition that = (AuthorizerDefinition) o;
        return scheme.equals(that.scheme)
                && Objects.equals(type, that.type)
                && Objects.equals(uri, that.uri)
                && Objects.equals(customAuthType, that.customAuthType)
                && Objects.equals(credentials, that.credentials)
                && Objects.equals(identitySource, that.identitySource)
                && Objects.equals(identityValidationExpression, that.identityValidationExpression)
                && Objects.equals(resultTtlInSeconds, that.resultTtlInSeconds)
                && Objects.equals(authorizerPayloadFormatVersion, that.authorizerPayloadFormatVersion)
                && Objects.equals(enableSimpleResponses, that.enableSimpleResponses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, type, uri);
    }

    /**
     * Builder used to create an {@link AuthorizerDefinition}.
     */
    public static final class Builder implements SmithyBuilder<AuthorizerDefinition> {
        private ShapeId scheme;
        private String type;
        private String customAuthType;
        private String uri;
        private String credentials;
        private String identitySource;
        private String identityValidationExpression;
        private Integer resultTtlInSeconds;
        private String authorizerPayloadFormatVersion;
        private Boolean enableSimpleResponses;

        @Override
        public AuthorizerDefinition build() {
            return new AuthorizerDefinition(this);
        }

        /**
         * Sets the client authentication scheme name.
         *
         * @param scheme Client authentication scheme.
         * @return Returns the builder.
         */
        public Builder scheme(ShapeId scheme) {
            this.scheme = scheme;
            return this;
        }

        /**
         * Sets the type of the authorizer.
         *
         * <p>If specifying information beyond the scheme, this value is
         * required. The value must be "token", for an authorizer with the
         * caller identity embedded in an authorization token, or "request",
         * for an authorizer with the caller identity contained in request
         * parameters.
         *
         * @param type authorizer type.
         * @return Returns the builder.
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the customAuthType of the authorizer.
         *
         * <p>This value is not used directly by APIGateway but will be used
         * for OpenAPI exports. This will default to "awsSigV4" if your scheme
         * is "aws.v4", or "custom" otherwise.</p>
         *
         * @param customAuthType the auth type (e.g. awsSigV4)
         * @return Returns the builder.
         */
        public Builder customAuthType(String customAuthType) {
            this.customAuthType = customAuthType;
            return this;
        }

        /**
         * Sets the Uniform Resource Identifier (URI) of the authorizer
         * Lambda function.
         *
         * <p>The syntax is as follows:
         *
         * @param uri the Lambda URI to set.
         * @return Returns the builder.
         */
        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Sets the Credentials required for invoking the authorizer, if any, in
         * the form of an ARN of an IAM execution role.
         *
         * <p>For example, "arn:aws:iam::account-id:IAM_role".
         *
         * @param credentials Credentials ARN to set.
         * @return Returns the builder.
         */
        public Builder credentials(String credentials) {
            this.credentials = credentials;
            return this;
        }

        /**
         * Sets the comma-separated list of mapping expressions of the request
         * parameters as the identity source.
         *
         * <p>This property is only applicable for the authorizer of the
         * "request" type only.
         *
         * @param identitySource Identity source CSV to set.
         * @return Returns the builder.
         */
        public Builder identitySource(String identitySource) {
            this.identitySource = identitySource;
            return this;
        }

        /**
         * Sets the regular expression for validating the token as the incoming
         * identity. For example, {@code "^x-[a-z]+"}.
         *
         * @param identityValidationExpression Expression to set.
         * @return Returns the builder.
         */
        public Builder identityValidationExpression(String identityValidationExpression) {
            this.identityValidationExpression = identityValidationExpression;
            return this;
        }

        /**
         * Sets the number of seconds during which the resulting IAM policy
         * is cached.
         *
         * @param resultTtlInSeconds Number of seconds to cache.
         * @return Returns the builder.
         */
        public Builder resultTtlInSeconds(Integer resultTtlInSeconds) {
            this.resultTtlInSeconds = resultTtlInSeconds;
            return this;
        }

        /**
         * Sets the format of the payload returned by the authorizer.
         *
         * @param authorizerPayloadFormatVersion format of the payload.
         * @return Returns the builder.
         */
        public Builder authorizerPayloadFormatVersion(String authorizerPayloadFormatVersion) {
            this.authorizerPayloadFormatVersion = authorizerPayloadFormatVersion;
            return this;
        }

        /**
         * Sets whether the authorizer returns simple responses.
         *
         * @param enableSimpleResponses defines if authorizer should return simple responses.
         * @return Returns the builder.
         */
        public Builder enableSimpleResponses(Boolean enableSimpleResponses) {
            this.enableSimpleResponses = enableSimpleResponses;
            return this;
        }
    }
}

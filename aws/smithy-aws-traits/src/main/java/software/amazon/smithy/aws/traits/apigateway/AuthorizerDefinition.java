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

package software.amazon.smithy.aws.traits.apigateway;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents an API Gateway authorizer.
 *
 * @see AuthorizersTrait
 */
public final class AuthorizerDefinition implements ToNode, ToSmithyBuilder<AuthorizerDefinition> {
    private static final String SCHEME_KEY = "scheme";
    private static final String TYPE_KEY = "type";
    private static final String URI_KEY = "uri";
    private static final String CREDENTIALS_KEY = "credentials";
    private static final String IDENTITY_SOURCE_KEY = "identitySource";
    private static final String IDENTITY_VALIDATION_EXPRESSION_KEY = "identityValidationExpression";
    private static final String RESULT_TTL_IN_SECONDS = "resultTtlInSeconds";
    private static final List<String> PROPERTIES = ListUtils.of(
            SCHEME_KEY, TYPE_KEY, URI_KEY, CREDENTIALS_KEY, IDENTITY_SOURCE_KEY,
            IDENTITY_VALIDATION_EXPRESSION_KEY, RESULT_TTL_IN_SECONDS);

    private final String scheme;
    private final String type;
    private final String uri;
    private final String credentials;
    private final String identitySource;
    private final String identityValidationExpression;
    private final Integer resultTtlInSeconds;

    private AuthorizerDefinition(Builder builder) {
        scheme = SmithyBuilder.requiredState(SCHEME_KEY, builder.scheme);
        type = builder.type;
        uri = builder.uri;
        credentials = builder.credentials;
        identitySource = builder.identitySource;
        identityValidationExpression = builder.identityValidationExpression;
        resultTtlInSeconds = builder.resultTtlInSeconds;
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
    public String getScheme() {
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

    @Override
    public Builder toBuilder() {
        return builder()
                .scheme(scheme)
                .type(type)
                .uri(uri)
                .credentials(credentials)
                .identitySource(identitySource)
                .identityValidationExpression(identityValidationExpression)
                .resultTtlInSeconds(resultTtlInSeconds);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember(SCHEME_KEY, Node.from(getScheme()))
                .withOptionalMember(TYPE_KEY, getType().map(Node::from))
                .withOptionalMember(URI_KEY, getUri().map(Node::from))
                .withOptionalMember(CREDENTIALS_KEY, getCredentials().map(Node::from))
                .withOptionalMember(IDENTITY_SOURCE_KEY, getIdentitySource().map(Node::from))
                .withOptionalMember(IDENTITY_VALIDATION_EXPRESSION_KEY,
                                    getIdentityValidationExpression().map(Node::from))
                .withOptionalMember(RESULT_TTL_IN_SECONDS, getResultTtlInSeconds().map(Node::from))
                .build();
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
               && Objects.equals(credentials, that.credentials)
               && Objects.equals(identitySource, that.identitySource)
               && Objects.equals(identityValidationExpression, that.identityValidationExpression)
               && Objects.equals(resultTtlInSeconds, that.resultTtlInSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, type, uri);
    }

    static AuthorizerDefinition fromNode(ObjectNode node) {
        node.warnIfAdditionalProperties(PROPERTIES);
        Builder builder = builder();
        builder.scheme(node.expectStringMember(SCHEME_KEY).getValue());
        node.getStringMember(TYPE_KEY)
                .map(StringNode::getValue)
                .ifPresent(builder::type);
        node.getStringMember(URI_KEY)
                .map(StringNode::getValue)
                .ifPresent(builder::uri);
        node.getStringMember(CREDENTIALS_KEY)
                .map(StringNode::getValue)
                .ifPresent(builder::credentials);
        node.getStringMember(IDENTITY_SOURCE_KEY)
                .map(StringNode::getValue)
                .ifPresent(builder::identitySource);
        node.getStringMember(IDENTITY_VALIDATION_EXPRESSION_KEY)
                .map(StringNode::getValue)
                .ifPresent(builder::identityValidationExpression);
        node.getNumberMember(RESULT_TTL_IN_SECONDS)
                .map(NumberNode::getValue)
                .map(Number::intValue)
                .ifPresent(builder::resultTtlInSeconds);
        return builder.build();
    }

    /**
     * Builder used to create an {@link AuthorizerDefinition}.
     */
    public static final class Builder implements SmithyBuilder<AuthorizerDefinition> {
        private String scheme;
        private String type;
        private String uri;
        private String credentials;
        private String identitySource;
        private String identityValidationExpression;
        private Integer resultTtlInSeconds;

        @Override
        public AuthorizerDefinition build() {
            return new AuthorizerDefinition(this);
        }

        /**
         * Sets the client authentication scheme name.
         *
         * @param scheme Client authentication scheme (e.g., aws.v4).
         * @return Returns the builder.
         */
        public Builder scheme(String scheme) {
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
    }
}

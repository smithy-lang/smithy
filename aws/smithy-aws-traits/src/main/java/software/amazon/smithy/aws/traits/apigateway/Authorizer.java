package software.amazon.smithy.aws.traits.apigateway;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.ListUtils;

/**
 * Represents an API Gateway authorizer.
 *
 * @see AuthorizersTrait
 */
public final class Authorizer implements ToNode, ToSmithyBuilder<Authorizer> {
    private static final String CLIENT_TYPE_KEY = "clientType";
    private static final String TYPE_KEY = "type";
    private static final String URI_KEY = "uri";
    private static final String CREDENTIALS_KEY = "credentials";
    private static final String IDENTITY_SOURCE_KEY = "identitySource";
    private static final String IDENTITY_VALIDATION_EXPRESSION_KEY = "identityValidationExpression";
    private static final String RESULT_TTL_IN_SECONDS = "resultTtlInSeconds";
    private static final List<String> PROPERTIES = ListUtils.of(
            CLIENT_TYPE_KEY, TYPE_KEY, URI_KEY, CREDENTIALS_KEY, IDENTITY_SOURCE_KEY,
            IDENTITY_VALIDATION_EXPRESSION_KEY, RESULT_TTL_IN_SECONDS);

    private final String clientType;
    private final String type;
    private final String uri;
    private final String credentials;
    private final String identitySource;
    private final String identityValidationExpression;
    private final Integer resultTtlInSeconds;

    private Authorizer(Builder builder) {
        clientType = builder.clientType;
        type = SmithyBuilder.requiredState(TYPE_KEY, builder.type);
        uri = SmithyBuilder.requiredState(URI_KEY, builder.uri);
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
     * Gets the client authentication type.
     *
     * <p>A value identifying the category of authorizer, such
     * as "oauth2" or "awsSigv4."
     *
     * @return Returns the optionally defined client authentication type.
     */
    public Optional<String> getClientType() {
        return Optional.ofNullable(clientType);
    }

    /**
     * Gets the type of the authorizer.
     *
     * <p>This is a required property and the value must be "token",
     * for an authorizer with the caller identity embedded in an
     * authorization token, or "request", for an authorizer with the
     * caller identity contained in request parameters.
     *
     * @return Returns the authorizer type.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the Uniform Resource Identifier (URI) of the authorizer
     * Lambda function.
     *
     * @return Returns the Lambda URI.
     */
    public String getUri() {
        return uri;
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
                .clientType(clientType)
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
                .withOptionalMember(CLIENT_TYPE_KEY, getClientType().map(Node::from))
                .withMember(TYPE_KEY, Node.from(getType()))
                .withMember(URI_KEY, Node.from(getUri()))
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
        } else if (!(o instanceof Authorizer)) {
            return false;
        }

        Authorizer that = (Authorizer) o;
        return Objects.equals(clientType, that.clientType)
               && type.equals(that.type)
               && uri.equals(that.uri)
               && Objects.equals(credentials, that.credentials)
               && Objects.equals(identitySource, that.identitySource)
               && Objects.equals(identityValidationExpression, that.identityValidationExpression)
               && Objects.equals(resultTtlInSeconds, that.resultTtlInSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientType, type, uri);
    }

    static Authorizer fromNode(ObjectNode node) {
        node.warnIfAdditionalProperties(PROPERTIES);
        Builder builder = builder();
        node.getStringMember(CLIENT_TYPE_KEY)
                .map(StringNode::getValue)
                .ifPresent(builder::clientType);
        builder.type(node.expectMember(TYPE_KEY).expectStringNode().getValue());
        builder.uri(node.expectMember(URI_KEY).expectStringNode().getValue());
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
     * Builder used to create an {@link Authorizer}.
     */
    public static final class Builder implements SmithyBuilder<Authorizer> {
        private String clientType;
        private String type;
        private String uri;
        private String credentials;
        private String identitySource;
        private String identityValidationExpression;
        private Integer resultTtlInSeconds;

        @Override
        public Authorizer build() {
            return new Authorizer(this);
        }

        /**
         * Sets the client authentication type.
         *
         * @param clientType Client authentication type such as "oauth2" or "awsSigv4."
         * @return Returns the builder.
         */
        public Builder clientType(String clientType) {
            this.clientType = clientType;
            return this;
        }

        /**
         * Sets the type of the authorizer.
         *
         * <p>This is a required property and the value must be "token",
         * for an authorizer with the caller identity embedded in an
         * authorization token, or "request", for an authorizer with the
         * caller identity contained in request parameters.
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

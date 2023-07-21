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

package software.amazon.smithy.aws.traits;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Registers a service as an AWS service. This trait is required for all AWS
 * services modeled in Smithy.
 */
public final class ServiceTrait extends AbstractTrait implements ToSmithyBuilder<ServiceTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#service");
    private static final Logger LOGGER = Logger.getLogger(ServiceTrait.class.getName());

    private final ShapeId target;
    private final String cloudFormationName;
    private final String arnNamespace;
    private final String sdkId;
    private final String cloudTrailEventSource;
    private final String docId;
    private final String endpointPrefix;

    private ServiceTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.target = SmithyBuilder.requiredState("target", builder.target);
        this.sdkId = SmithyBuilder.requiredState("sdkId", builder.sdkId);
        this.arnNamespace = SmithyBuilder.requiredState("arnNamespace", builder.arnNamespace);
        this.cloudFormationName = SmithyBuilder.requiredState("cloudFormationName", builder.cloudFormationName);
        this.cloudTrailEventSource = SmithyBuilder.requiredState(
                "cloudTrailEventSource", builder.cloudTrailEventSource);
        this.docId = builder.docId;
        this.endpointPrefix = SmithyBuilder.requiredState("endpointPrefix", builder.endpointPrefix);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);
            String sdkId = objectNode.getStringMember("sdkId")
                    .map(StringNode::getValue)
                    .orElseThrow(() -> new SourceException(String.format(
                            "No sdkId was provided. Perhaps you could set this to %s?",
                            target.getName()), value));
            builder.sdkId(sdkId);
            objectNode.getStringMember("arnNamespace")
                    .map(StringNode::getValue)
                    .ifPresent(builder::arnNamespace);
            objectNode.getStringMember("cloudFormationName")
                    .map(StringNode::getValue)
                    .ifPresent(builder::cloudFormationName);
            objectNode.getStringMember("cloudTrailEventSource").map(StringNode::getValue)
                    .ifPresent(builder::cloudTrailEventSource);
            objectNode.getStringMember("docId").map(StringNode::getValue)
                    .ifPresent(builder::docId);
            objectNode.getStringMember("endpointPrefix")
                    .map(StringNode::getValue)
                    .ifPresent(builder::endpointPrefix);
            ServiceTrait result = builder.build(target);
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * @return Creates a builder used to build a {@link ServiceTrait}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the AWS ARN service namespace of the service.
     *
     * <p>If not set, this value defaults to the name of the service shape
     * converted to lowercase. This value is combined with resources contained
     * within the service to form ARNs for resources. Only resources that
     * explicitly define the 'aws.api#arnTemplate' trait are assigned ARNs,
     * and their relative ARNs are combined with the service's arnNamespace to
     * form an ARN.
     *
     * @return Returns the ARN service name (e.g., "route53").
     */
    public String getArnNamespace() {
        return arnNamespace;
    }

    /**
     * Get the AWS CloudFormation service name.
     *
     * <p>When not set, this value defaults to the name of the service shape.
     *
     * @return Returns the optionally present AWS CloudFormation type prefix.
     */
    public String getCloudFormationName() {
        return cloudFormationName;
    }

    /**
     * Get the SDK service ID.
     *
     * <p>This value is used to generate SDK class names.
     *
     * @return Returns the AWS SDK service ID value.
     */
    public String getSdkId() {
        return sdkId;
    }

    /**
     * Returns the CloudTrail event source name of the service.
     *
     * @return Returns the event source name.
     */
    public String getCloudTrailEventSource() {
        return cloudTrailEventSource;
    }

    /**
     * Returns the documentation identifier value for the service.
     *
     * <p> When value on trait is not set, this method defaults to the lower
     * cased value of the sdkId followed by the service version, separated by
     * dashes.
     *
     * @return Returns the documentation identifier value for the service name.
     */
    public String getDocId(Model model) {
        return getDocId().orElseGet(() -> buildDefaultDocId(model));
    }

    protected Optional<String> getDocId() {
        return Optional.ofNullable(docId);
    }

    private String buildDefaultDocId(Model model) {
        String version = model.expectShape(target, ServiceShape.class).getVersion();
        return sdkId.replace(" ", "-").toLowerCase(Locale.US) + "-" + version;
    }

    /**
     * Returns the endpoint prefix for the service.
     *
     * This value is not unique across services and it can change at any time.
     * Therefore it MUST NOT be used to generate class names, namespaces, or
     * for any other purpose that requires a static, unique identifier. The
     * sdkId property should be used for those purposes.
     *
     * @return Returns the aws sdk endpoint prefix.
     */
    public String getEndpointPrefix() {
        return endpointPrefix;
    }

    @Deprecated
    public Optional<String> getAbbreviation() {
        return Optional.empty();
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .target(target)
                .sdkId(sdkId)
                .sourceLocation(getSourceLocation())
                .cloudFormationName(cloudFormationName)
                .arnNamespace(arnNamespace)
                .cloudTrailEventSource(cloudTrailEventSource)
                .docId(docId)
                .endpointPrefix(endpointPrefix);
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember("target", Node.from(target.getName()))
                .withMember("sdkId", Node.from(sdkId))
                .withMember("arnNamespace", Node.from(getArnNamespace()))
                .withMember("cloudFormationName", Node.from(getCloudFormationName()))
                .withMember("cloudTrailEventSource", Node.from(getCloudTrailEventSource()))
                .withOptionalMember("docId", getDocId().map(Node::from))
                .withMember("endpointPrefix", Node.from(getEndpointPrefix()))
                .build();
    }

    // Due to the defaulting of this trait, equals has to be overridden
    // so that inconsequential differences in toNode do not effect equality.
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ServiceTrait)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            ServiceTrait os = (ServiceTrait) other;
            return sdkId.equals(os.sdkId)
                    && target.equals(os.target)
                    && arnNamespace.equals(os.arnNamespace)
                    && cloudFormationName.equals(os.cloudFormationName)
                    && cloudTrailEventSource.equals(os.cloudTrailEventSource)
                    && docId == null
                        ? os.docId == null
                        : docId.equals(os.docId)
                    && endpointPrefix.equals(os.endpointPrefix);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(toShapeId(), target, sdkId, arnNamespace, cloudFormationName,
                            cloudTrailEventSource, docId, endpointPrefix);
    }

    /** Builder for {@link ServiceTrait}. */
    public static final class Builder extends AbstractTraitBuilder<ServiceTrait, Builder> {
        private ShapeId target;
        private String sdkId;
        private String cloudFormationName;
        private String arnNamespace;
        private String cloudTrailEventSource;
        private String docId;
        private String endpointPrefix;

        private Builder() {}

        @Override
        public ServiceTrait build() {
            if (endpointPrefix == null) {
                endpointPrefix(arnNamespace);
            }

            return new ServiceTrait(this);
        }

        public ServiceTrait build(ShapeId target) {
            this.target = target;

            // Fill in default values if they weren't set.
            if (arnNamespace == null) {
                arnNamespace(target.getName().toLowerCase(Locale.US));
            }

            if (cloudFormationName == null) {
                cloudFormationName(target.getName());
            }

            if (cloudTrailEventSource == null) {
                cloudTrailEventSource(arnNamespace + ".amazonaws.com");
            }

            if (endpointPrefix == null) {
                endpointPrefix(arnNamespace);
            }

            return new ServiceTrait(this);
        }

        /**
         * Sets the target shape to which the trait is applied.
         *
         * @param target the ShapeId targeted by the trait.
         * @return Returns the builder.
         */
        private Builder target(ShapeId target) {
            this.target = target;
            return this;
        }

        /**
         * Sets the AWS CloudFormation resource type service name.
         *
         * <p>Must match the following regex {@code ^[a-z0-9.\-]{1,63}$}
         *
         * @param cloudFormationName AWS CloudFormation resource type service name.
         * @return Returns the builder.
         */
        public Builder cloudFormationName(String cloudFormationName) {
            this.cloudFormationName = cloudFormationName;
            return this;
        }

        /**
         * Set the ARN service namespace of the service.
         *
         * <p>Must match the following regex: {@code ^[A-Z][A-Za-z0-9]+$}
         *
         * @param arnNamespace ARN service namespace to set.
         * @return Returns the builder.
         */
        public Builder arnNamespace(String arnNamespace) {
            this.arnNamespace = arnNamespace;
            return this;
        }

        /**
         * Set the SDK service ID trait used to control client class names.
         *
         * <p>Must match the following regex: {@code ^[a-zA-Z][a-zA-Z0-9]*( [a-zA-Z0-9]+)*$}
         *
         * @param sdkId SDK service ID to set.
         * @return Returns the builder.
         */
        public Builder sdkId(String sdkId) {
            this.sdkId = sdkId;
            return this;
        }

        /**
         * Set the CloudTrail event source name of the service.
         *
         * @param cloudTrailEventSource CloudTrail event source name of the service.
         * @return Returns the builder.
         */
        public Builder cloudTrailEventSource(String cloudTrailEventSource) {
            this.cloudTrailEventSource = cloudTrailEventSource;
            return this;
        }

        /**
         * Set the documentation identifier for the service.
         *
         * @param docId documentation identifier for the service.
         * @return Returns the builder.
         */
        public Builder docId(String docId) {
            this.docId = docId;
            return this;
        }

        /**
         * Set the endpoint prefix used to construct client endpoints.
         *
         * @param endpointPrefix The endpoint prefix of the service.
         * @return Returns the builder.
         */
        public Builder endpointPrefix(String endpointPrefix) {
            this.endpointPrefix = endpointPrefix;
            return this;
        }

        @Deprecated
        public Builder abbreviation(String abbreviation) {
            LOGGER.warning("The `abbreviation` property of aws.api#service is not supported");
            return this;
        }
    }
}

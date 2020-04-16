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
import java.util.Optional;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
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

    private final String abbreviation;
    private final String cloudFormationName;
    private final String arnNamespace;
    private final String sdkId;
    private final String cloudTrailEventSource;

    private ServiceTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.sdkId = SmithyBuilder.requiredState("sdkId", builder.sdkId);
        this.arnNamespace = SmithyBuilder.requiredState("arnNamespace", builder.arnNamespace);
        this.cloudFormationName = SmithyBuilder.requiredState("cloudFormationName", builder.cloudFormationName);
        this.cloudTrailEventSource = SmithyBuilder.requiredState(
                "cloudTrailEventSource", builder.cloudTrailEventSource);
        this.abbreviation = builder.abbreviation;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder();
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
            objectNode.getStringMember("abbreviation").map(StringNode::getValue)
                    .ifPresent(builder::abbreviation);
            return builder.build(target);
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
     * Gets the abbreviated name of the service (if available).
     *
     * @return Returns the service abbreviation.
     */
    public Optional<String> getAbbreviation() {
        return Optional.ofNullable(abbreviation);
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .sdkId(sdkId)
                .sourceLocation(getSourceLocation())
                .cloudFormationName(cloudFormationName)
                .arnNamespace(arnNamespace)
                .cloudTrailEventSource(cloudTrailEventSource)
                .abbreviation(abbreviation);
    }

    @Override
    protected Node createNode() {
        return Node.objectNode()
                .withMember("sdkId", Node.from(sdkId))
                .withMember("arnNamespace", Node.from(getArnNamespace()))
                .withMember("cloudFormationName", Node.from(getCloudFormationName()))
                .withMember("cloudTrailEventSource", Node.from(getCloudTrailEventSource()))
                .withOptionalMember("abbreviation", getAbbreviation().map(Node::from));
    }

    /** Builder for {@link ServiceTrait}. */
    public static final class Builder extends AbstractTraitBuilder<ServiceTrait, Builder> {
        private String abbreviation;
        private String sdkId;
        private String cloudFormationName;
        private String arnNamespace;
        private String cloudTrailEventSource;

        private Builder() {}

        @Override
        public ServiceTrait build() {
            return new ServiceTrait(this);
        }

        public ServiceTrait build(ShapeId target) {
            // Fill in default values if they weren't set.
            if (arnNamespace == null) {
                arnNamespace(target.getName().toLowerCase(Locale.US));
            }

            if (cloudFormationName == null) {
                cloudFormationName(target.getName());
            }

            if (cloudTrailEventSource == null) {
                cloudTrailEventSource = arnNamespace + ".amazonaws.com";
            }

            return new ServiceTrait(this);
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
         * Sets the abbreviated name of the service.
         *
         * @param abbreviation Abbreviated service name.
         * @return Returns the builder.
         */
        public Builder abbreviation(String abbreviation) {
            this.abbreviation = abbreviation;
            return this;
        }
    }
}

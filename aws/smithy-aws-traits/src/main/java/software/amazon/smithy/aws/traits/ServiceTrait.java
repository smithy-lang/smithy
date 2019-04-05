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

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;

/**
 * Registers a service as an AWS service. This trait is required for all AWS
 * services modeled in Smithy.
 */
public final class ServiceTrait extends AbstractTrait implements ToSmithyBuilder<ServiceTrait> {
    private static final Logger LOGGER = Logger.getLogger(ServiceTrait.class.getName());
    private static final String TRAIT = "aws.api#service";

    private final String abbreviation;
    private final String cloudFormationName;
    private final String arnNamespace;
    private final String sdkId;
    private final String cloudTrailEventSource;

    private ServiceTrait(Builder builder) {
        super(TRAIT, builder.getSourceLocation());
        this.sdkId = SmithyBuilder.requiredState("sdkId", builder.sdkId);
        this.arnNamespace = SmithyBuilder.requiredState("arnNamespace", builder.arnNamespace);
        this.cloudFormationName = SmithyBuilder.requiredState("cloudFormationName", builder.cloudFormationName);
        this.cloudTrailEventSource = SmithyBuilder.requiredState(
                "cloudTrailEventSource", builder.cloudTrailEventSource);
        this.abbreviation = builder.abbreviation;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(TRAIT);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            var objectNode = value.expectObjectNode();
            objectNode.warnIfAdditionalProperties(Arrays.asList(
                    "sdkId", "arnNamespace", "cloudFormationName", "cloudTrailEventSource", "abbreviation"));

            var builder = builder();
            String sdkId = getOneStringValue(objectNode, "sdkId", "sdkServiceId")
                    .orElseThrow(() -> new SourceException(String.format(
                            "No sdkId was provided. Perhaps you could set this to %s?",
                            target.getName()), value));
            builder.sdkId(sdkId);
            getOneStringValue(objectNode, "arnNamespace", "arnService")
                    .ifPresent(builder::arnNamespace);
            getOneStringValue(objectNode, "cloudFormationName", "productName")
                    .ifPresent(builder::cloudFormationName);
            objectNode.getStringMember("cloudTrailEventSource").map(StringNode::getValue)
                    .ifPresent(builder::cloudTrailEventSource);
            objectNode.getStringMember("abbreviation").map(StringNode::getValue)
                    .ifPresent(builder::abbreviation);
            return builder.build(target);
        }
    }

    private static Optional<String> getOneStringValue(ObjectNode object, String key1, String key2) {
        return object.getStringMember(key1)
                .or(() -> {
                    var result = object.getStringMember(key2);
                    if (result.isPresent()) {
                        LOGGER.warning(() -> "The `" + TRAIT + "` property `" + key2 + "` is deprecated. Use `"
                                             + key1 + "` instead.");
                    }
                    return result;
                })
                .map(StringNode::getValue);
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

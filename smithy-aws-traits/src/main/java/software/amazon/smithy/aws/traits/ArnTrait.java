/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Configures the ARN template of a resource shape, relative to the
 * service to which a resource is bound.
 */
public final class ArnTrait extends AbstractTrait implements ToSmithyBuilder<ArnTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#arn");

    private static final String TEMPLATE = "template";
    private static final String ABSOLUTE = "absolute";
    private static final String NO_REGION = "noRegion";
    private static final String NO_ACCOUNT = "noAccount";
    private static final String RESOURCE_DELIMITER = "resourceDelimiter";
    private static final String REUSABLE = "reusable";
    private static final Pattern PATTERN = Pattern.compile("\\{([^}]+)}");

    private final boolean noRegion;
    private final boolean noAccount;
    private final boolean absolute;
    private final String template;
    private final List<String> labels;
    private final ResourceDelimiter resourceDelimiter;
    private final boolean reusable;

    private ArnTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.template = SmithyBuilder.requiredState(TEMPLATE, builder.template);
        this.labels = Collections.unmodifiableList(parseLabels(template));
        this.noRegion = builder.noRegion;
        this.noAccount = builder.noAccount;
        this.absolute = builder.absolute;
        this.resourceDelimiter = builder.resourceDelimiter;
        this.reusable = builder.reusable;

        if (template.startsWith("/")) {
            throw new SourceException("Invalid aws.api#arn trait. The template must not start with '/'. "
                    + "Found `" + template + "`", getSourceLocation());
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);
            builder.template(objectNode.expectStringMember(TEMPLATE).getValue());
            builder.absolute(objectNode.getBooleanMemberOrDefault(ABSOLUTE));
            builder.noRegion(objectNode.getBooleanMemberOrDefault(NO_REGION));
            builder.noAccount(objectNode.getBooleanMemberOrDefault(NO_ACCOUNT));
            objectNode.getStringMember(RESOURCE_DELIMITER, builder::resourceDelimiter);
            builder.reusable(objectNode.getBooleanMemberOrDefault(REUSABLE));
            ArnTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    private static List<String> parseLabels(String resource) {
        List<String> result = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(resource);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    /**
     * @return Returns a builder used to create {@link ArnTrait}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Returns the noAccount setting.
     */
    public boolean isNoAccount() {
        return noAccount;
    }

    /**
     * @return Returns the noRegion setting.
     */
    public boolean isNoRegion() {
        return noRegion;
    }

    /**
     * @return Returns whether or not the ARN is absolute.
     */
    public boolean isAbsolute() {
        return absolute;
    }

    /**
     * @return Gets the template of the ARN.
     */
    public String getTemplate() {
        return template;
    }

    /**
     * @return Returns the label placeholder variable names.
     */
    public List<String> getLabels() {
        return labels;
    }

    /**
     * @return Returns the resource delimiter for absolute ARNs.
     */
    public Optional<ResourceDelimiter> getResourceDelimiter() {
        return Optional.ofNullable(resourceDelimiter);
    }

    /**
     * @return Returns if the ARN may be reused for different instances of a resource.
     */
    public boolean isReusable() {
        return reusable;
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember(TEMPLATE, Node.from(getTemplate()))
                .withMember(ABSOLUTE, Node.from(isAbsolute()))
                .withMember(NO_ACCOUNT, Node.from(isNoAccount()))
                .withMember(NO_REGION, Node.from(isNoRegion()))
                .withOptionalMember(RESOURCE_DELIMITER, getResourceDelimiter())
                .withMember(REUSABLE, Node.from(isReusable()))
                .build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .noRegion(isNoRegion())
                .noAccount(isNoAccount())
                .absolute(isAbsolute())
                .template(getTemplate())
                .resourceDelimiter(resourceDelimiter)
                .reusable(reusable);
    }

    // Due to the defaulting of this trait, equals has to be overridden
    // so that inconsequential differences in toNode do not affect equality.
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ArnTrait)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            ArnTrait oa = (ArnTrait) other;
            return template.equals(oa.template)
                    && absolute == oa.absolute
                    && noAccount == oa.noAccount
                    && noRegion == oa.noRegion
                    && resourceDelimiter == oa.resourceDelimiter
                    && reusable == oa.reusable;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(toShapeId(), template, absolute, noAccount, noRegion, resourceDelimiter, reusable);
    }

    public enum ResourceDelimiter implements ToNode {
        FORWARD_SLASH("/"),
        COLON(":");

        private final String value;

        ResourceDelimiter(String value) {
            this.value = value;
        }

        static ResourceDelimiter from(String value) {
            for (ResourceDelimiter delimiter : values()) {
                if (delimiter.value.equals(value)) {
                    return delimiter;
                }
            }
            throw new IllegalArgumentException("Invalid delimiter: " + value);
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.from(value);
        }
    }

    /** Builder for {@link ArnTrait}. */
    public static final class Builder extends AbstractTraitBuilder<ArnTrait, Builder> {
        private boolean noRegion;
        private boolean noAccount;
        private boolean absolute;
        private String template;
        private ResourceDelimiter resourceDelimiter;
        private boolean reusable;

        private Builder() {}

        @Override
        public ArnTrait build() {
            return new ArnTrait(this);
        }

        public Builder template(String template) {
            this.template = template;
            return this;
        }

        public Builder absolute(boolean absolute) {
            this.absolute = absolute;
            return this;
        }

        public Builder noAccount(boolean noAccount) {
            this.noAccount = noAccount;
            return this;
        }

        public Builder noRegion(boolean noRegion) {
            this.noRegion = noRegion;
            return this;
        }

        public Builder resourceDelimiter(ResourceDelimiter resourceDelimiter) {
            this.resourceDelimiter = resourceDelimiter;
            return this;
        }

        public Builder resourceDelimiter(String resourceDelimiter) {
            this.resourceDelimiter = ResourceDelimiter.from(resourceDelimiter);
            return this;
        }

        public Builder reusable(boolean reusable) {
            this.reusable = reusable;
            return this;
        }
    }
}

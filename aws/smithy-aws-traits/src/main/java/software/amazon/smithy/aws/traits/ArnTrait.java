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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.TraitService;

/**
 * Configures the ARN template of a resource shape, relative to the
 * service to which a resource is bound.
 */
public final class ArnTrait extends AbstractTrait implements ToSmithyBuilder<ArnTrait> {
    private static final String TRAIT = "aws.api#arn";
    private static final String TEMPLATE = "template";
    private static final String ABSOLUTE = "absolute";
    private static final String NO_REGION = "noRegion";
    private static final String NO_ACCOUNT = "noAccount";
    private static final List<String> PROPERTIES = List.of(TEMPLATE, ABSOLUTE, NO_REGION, NO_ACCOUNT);
    private static final Pattern PATTERN = Pattern.compile("\\{([^}]+)}");

    private final boolean noRegion;
    private final boolean noAccount;
    private final boolean absolute;
    private final String template;
    private final List<String> labels;

    private ArnTrait(Builder builder) {
        super(TRAIT, builder.getSourceLocation());
        this.template = SmithyBuilder.requiredState(TEMPLATE, builder.template);
        this.noRegion = builder.noRegion;
        this.noAccount = builder.noAccount;
        this.absolute = builder.absolute;
        this.labels = Collections.unmodifiableList(parseLabels(template));

        if (template.startsWith("/")) {
            throw new SourceException("Invalid aws.api#arn trait. The template must not start with '/'. "
                                      + "Found `" + template + "`", getSourceLocation());
        }
    }

    public static TraitService provider() {
        return TraitService.createProvider(TRAIT, (target, value) -> {
            var builder = builder();
            var objectNode = value.expectObjectNode();
            objectNode.warnIfAdditionalProperties(PROPERTIES);
            builder.template(objectNode.expectMember(TEMPLATE).expectStringNode().getValue());
            builder.absolute(objectNode.getMember(ABSOLUTE)
                    .map(Node::expectBooleanNode)
                    .map(BooleanNode::getValue)
                    .orElse(false));
            builder.noRegion(objectNode.getMember(NO_REGION)
                    .map(Node::expectBooleanNode)
                    .map(BooleanNode::getValue)
                    .orElse(false));
            builder.noAccount(objectNode.getMember(NO_ACCOUNT)
                    .map(Node::expectBooleanNode)
                    .map(BooleanNode::getValue)
                    .orElse(false));
            return builder.build();
        });
    }

    private static List<String> parseLabels(String resource) {
        List<String> result = new ArrayList<>();
        var matcher = PATTERN.matcher(resource);
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
     * @return Returns the label place holder variable names.
     */
    public List<String> getLabels() {
        return labels;
    }

    @Override
    protected Node createNode() {
        return Node.objectNode()
                .withMember(TEMPLATE, Node.from(getTemplate()))
                .withMember(ABSOLUTE, Node.from(isAbsolute()))
                .withMember(NO_ACCOUNT, Node.from(isNoAccount()))
                .withMember(NO_REGION, Node.from(isNoRegion()));
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .noRegion(isNoRegion())
                .noAccount(isNoAccount())
                .template(getTemplate());
    }

    /** Builder for {@link ArnTrait}. */
    public static final class Builder extends AbstractTraitBuilder<ArnTrait, Builder> {
        private boolean noRegion;
        private boolean noAccount;
        private boolean absolute;
        private String template;

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
    }
}

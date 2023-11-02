/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.iam.traits;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ConditionKeyDefinition implements ToNode, ToSmithyBuilder<ConditionKeyDefinition> {
    private static final String TYPE = "type";
    private static final String DOCUMENTATION = "documentation";
    private static final String EXTERNAL_DOCUMENTATION = "externalDocumentation";
    private static final String RELATIVE_DOCUMENTATION = "relativeDocumentation";

    private final String type;
    private final String documentation;
    private final String externalDocumentation;
    private final String relativeDocumentation;

    private ConditionKeyDefinition(Builder builder) {
        type = SmithyBuilder.requiredState(TYPE, builder.type);
        documentation = builder.documentation;
        externalDocumentation = builder.externalDocumentation;
        relativeDocumentation = builder.relativeDocumentation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ConditionKeyDefinition fromNode(Node value) {
        ObjectNode objectNode = value.expectObjectNode();
        Builder builder = builder()
                .type(objectNode.expectStringMember(TYPE).getValue());
        objectNode.getStringMember(DOCUMENTATION).map(StringNode::getValue)
                .ifPresent(builder::documentation);
        objectNode.getStringMember(EXTERNAL_DOCUMENTATION).map(StringNode::getValue)
                .ifPresent(builder::externalDocumentation);
        objectNode.getStringMember(RELATIVE_DOCUMENTATION).map(StringNode::getValue)
                .ifPresent(builder::relativeDocumentation);

        return builder.build();
    }

    /**
     * @return The IAM policy type of the value that will supplied for this condition key.
     */
    public String getType() {
        return type;
    }

    /**
     * @return A short description of the role of the condition key.
     */
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * @return A URL to the documentation page.
     */
    public Optional<String> getExternalDocumentation() {
        return Optional.ofNullable(externalDocumentation);
    }

    /**
     * Get the relative URL path that defines more information about the condition key
     * within a set of IAM-related documentation.
     *
     * @return A relative URL to the documentation page.
     */
    public Optional<String> getRelativeDocumentation() {
        return Optional.ofNullable(relativeDocumentation);
    }

    @Override
    public SmithyBuilder<ConditionKeyDefinition> toBuilder() {
        return builder()
                .documentation(documentation)
                .externalDocumentation(externalDocumentation)
                .relativeDocumentation(relativeDocumentation)
                .type(type);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember(TYPE, Node.from(type))
                .withOptionalMember(DOCUMENTATION, getDocumentation().map(Node::from))
                .withOptionalMember(EXTERNAL_DOCUMENTATION, getExternalDocumentation().map(Node::from))
                .withOptionalMember(RELATIVE_DOCUMENTATION, getRelativeDocumentation().map(Node::from))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConditionKeyDefinition that = (ConditionKeyDefinition) o;
        return Objects.equals(type, that.type)
               && Objects.equals(documentation, that.documentation)
               && Objects.equals(externalDocumentation, that.externalDocumentation)
               && Objects.equals(relativeDocumentation, that.relativeDocumentation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, documentation, externalDocumentation, relativeDocumentation);
    }

    public static final class Builder implements SmithyBuilder<ConditionKeyDefinition> {
        private String type;
        private String documentation;
        private String externalDocumentation;
        private String relativeDocumentation;

        @Override
        public ConditionKeyDefinition build() {
            return new ConditionKeyDefinition(this);
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder externalDocumentation(String externalDocumentation) {
            this.externalDocumentation = externalDocumentation;
            return this;
        }

        public Builder relativeDocumentation(String relativeDocumentation) {
            this.relativeDocumentation = relativeDocumentation;
            return this;
        }
    }
}

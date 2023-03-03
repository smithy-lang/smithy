/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.openapi.model;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ExampleObject extends Component implements ToSmithyBuilder<ExampleObject> {
    private final String summary;
    private final String description;
    private final Node value;
    private final String externalValue;

    private ExampleObject(Builder builder) {
        super(builder);
        this.summary = builder.summary;
        this.description = builder.description;
        this.value = builder.value;
        this.externalValue = builder.externalValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    // getters
    public Optional<String> getSummary() {
        return Optional.ofNullable(summary);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<Node> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<String> getExternalValue() {
        return Optional.ofNullable(externalValue);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .summary(summary)
                .description(description)
                .value(value)
                .externalValue(externalValue);
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        return Node.objectNodeBuilder()
                .withOptionalMember("summary", getSummary().map(Node::from))
                .withOptionalMember("description", getDescription().map(Node::from))
                .withOptionalMember("value", getValue().map(Node::from))
                .withOptionalMember("externalValue", getExternalValue().map(Node::from));
    }

    public static ExampleObject fromNode(Node exampleObject) {
        if (exampleObject == null) {
            return null;
        }
        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.IGNORE);
        ObjectNode node = exampleObject.expectObjectNode();
        ExampleObject.Builder result = new ExampleObject.Builder();
        mapper.deserializeInto(node, result);
        return result.build();
    }


    public static final class Builder extends Component.Builder<Builder, ExampleObject> {
        private String summary;
        private String description;
        private Node value;
        private String externalValue;

        private Builder() {}

        @Override
        public ExampleObject build() {
            return new ExampleObject(this);
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder value(Node value) {
            this.value = value;
            return this;
        }

        public Builder externalValue(String externalValue) {
            this.externalValue = externalValue;
            return this;
        }
    }
}

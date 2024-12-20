/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.model;

import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Data class representing a CloudFormation Resource Schema's remote definition.
 *
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-remote">Resource Handler Definition</a>
 * @see <a href="https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.jsonL349">Resource Type Remote JSON Schema</a>
 */
public final class Remote implements ToNode, ToSmithyBuilder<Remote> {
    private final Map<String, Schema> definitions = new TreeMap<>();
    private final Map<String, Property> properties = new TreeMap<>();

    private Remote(Builder builder) {
        properties.putAll(builder.properties);
        definitions.putAll(builder.definitions);
    }

    @Override
    public Node toNode() {
        NodeMapper mapper = new NodeMapper();
        ObjectNode.Builder builder = Node.objectNodeBuilder();

        if (!definitions.isEmpty()) {
            builder.withMember("definitions", mapper.serialize(definitions));
        }

        if (!properties.isEmpty()) {
            builder.withMember("properties", mapper.serialize(properties));
        }

        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .definitions(definitions)
                .properties(properties);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Schema> getDefinitions() {
        return definitions;
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    public static final class Builder implements SmithyBuilder<Remote> {
        private final Map<String, Schema> definitions = new TreeMap<>();
        private final Map<String, Property> properties = new TreeMap<>();

        private Builder() {}

        @Override
        public Remote build() {
            return new Remote(this);
        }

        public Builder definitions(Map<String, Schema> definitions) {
            this.definitions.clear();
            this.definitions.putAll(definitions);
            return this;
        }

        public Builder addDefinition(String name, Schema definition) {
            this.definitions.put(name, definition);
            return this;
        }

        public Builder removeDefinition(String name) {
            this.definitions.remove(name);
            return this;
        }

        public Builder clearDefinitions() {
            this.definitions.clear();
            return this;
        }

        public Builder properties(Map<String, Property> properties) {
            this.properties.clear();
            this.properties.putAll(properties);
            return this;
        }

        public Builder addProperty(String name, Property property) {
            this.properties.put(name, property);
            return this;
        }

        public Builder removeProperty(String name) {
            this.properties.remove(name);
            return this;
        }

        public Builder clearProperties() {
            this.properties.clear();
            return this;
        }
    }
}

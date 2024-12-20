/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ParameterObject extends Component implements ToSmithyBuilder<ParameterObject> {
    private final String name;
    private final String in;
    private final String description;
    private final boolean required;
    private final boolean deprecated;
    private final boolean allowEmptyValue;
    private final String style;
    private final boolean explode;
    private final boolean allowReserved;
    private final Schema schema;
    private final ExampleObject example;
    private final Map<String, ExampleObject> examples;
    private final Map<String, MediaTypeObject> content;

    private ParameterObject(Builder builder) {
        super(builder);
        name = builder.name;
        in = builder.in;
        description = builder.description;
        required = builder.required;
        deprecated = builder.deprecated;
        allowEmptyValue = builder.allowEmptyValue;
        style = builder.style;
        explode = builder.explode;
        allowReserved = builder.allowReserved;
        schema = builder.schema;
        example = builder.example;
        examples = Collections.unmodifiableMap(new TreeMap<>(builder.examples));
        content = Collections.unmodifiableMap(new TreeMap<>(builder.content));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getIn() {
        return Optional.ofNullable(in);
    }

    public Map<String, ExampleObject> getExamples() {
        return examples;
    }

    public Map<String, MediaTypeObject> getContent() {
        return content;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public boolean isAllowEmptyValue() {
        return allowEmptyValue;
    }

    public boolean isExplode() {
        return explode;
    }

    public boolean isAllowReserved() {
        return allowReserved;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getStyle() {
        return Optional.ofNullable(style);
    }

    public Optional<Schema> getSchema() {
        return Optional.ofNullable(schema);
    }

    public Optional<ExampleObject> getExample() {
        return Optional.ofNullable(example);
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember("name", getName().map(Node::from))
                .withOptionalMember("in", getIn().map(Node::from))
                .withOptionalMember("description", getDescription().map(Node::from))
                .withOptionalMember("style", getStyle().map(Node::from))
                .withOptionalMember("schema", getSchema())
                .withOptionalMember("example", getExample());

        if (isDeprecated()) {
            builder.withMember("deprecated", Node.from(true));
        }

        if (isAllowEmptyValue()) {
            builder.withMember("allowEmptyValue", Node.from(true));
        }

        if (isAllowReserved()) {
            builder.withMember("allowReserved", Node.from(true));
        }

        if (isExplode()) {
            builder.withMember("explode", Node.from(true));
        }

        if (isRequired()) {
            builder.withMember("required", Node.from(true));
        }

        if (!examples.isEmpty()) {
            builder.withMember("examples",
                    getExamples().entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!content.isEmpty()) {
            builder.withMember("content",
                    getContent().entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .extensions(getExtensions())
                .name(name)
                .in(in)
                .description(description)
                .required(required)
                .deprecated(deprecated)
                .allowEmptyValue(allowEmptyValue)
                .style(style)
                .explode(explode)
                .allowReserved(allowReserved)
                .schema(schema)
                .example(example == null ? null : example.toNode())
                .content(content);

        for (Map.Entry<String, ExampleObject> ex : examples.entrySet()) {
            builder.putExample(ex.getKey(), ex.getValue().toNode());
        }

        return builder;
    }

    public static final class Builder extends Component.Builder<Builder, ParameterObject> {
        private String name;
        private String in;
        private String description;
        private boolean required;
        private boolean deprecated;
        private boolean allowEmptyValue;
        private String style;
        private boolean explode;
        private boolean allowReserved;
        private Schema schema;
        private ExampleObject example;
        private final Map<String, ExampleObject> examples = new TreeMap<>();
        private final Map<String, MediaTypeObject> content = new TreeMap<>();

        private Builder() {}

        @Override
        public ParameterObject build() {
            return new ParameterObject(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder in(String in) {
            this.in = in;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder allowEmptyValue(boolean allowEmptyValue) {
            this.allowEmptyValue = allowEmptyValue;
            return this;
        }

        public Builder style(String style) {
            this.style = style;
            return this;
        }

        public Builder explode(boolean explode) {
            this.explode = explode;
            return this;
        }

        public Builder allowReserved(boolean allowReserved) {
            this.allowReserved = allowReserved;
            return this;
        }

        public Builder schema(Schema schema) {
            this.schema = schema;
            return this;
        }

        public Builder example(Node example) {
            this.example = ExampleObject.fromNode(example);
            return this;
        }

        public Builder examples(Map<String, Node> examples) {
            this.examples.clear();
            for (Map.Entry<String, Node> example : examples.entrySet()) {
                this.examples.put(example.getKey(), ExampleObject.fromNode(example.getValue()));
            }
            return this;
        }

        public Builder putExample(String name, Node example) {
            this.examples.put(name, ExampleObject.fromNode(example));
            return this;
        }

        public Builder content(Map<String, MediaTypeObject> content) {
            this.content.clear();
            this.content.putAll(content);
            return this;
        }

        public Builder putContent(String name, MediaTypeObject mediaTypeObject) {
            this.content.put(name, mediaTypeObject);
            return this;
        }
    }
}

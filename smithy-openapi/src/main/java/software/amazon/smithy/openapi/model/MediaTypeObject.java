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

package software.amazon.smithy.openapi.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class MediaTypeObject extends Component implements ToSmithyBuilder<MediaTypeObject> {
    private final Schema schema;
    private final ExampleObject example;
    private final Map<String, ExampleObject> examples = new TreeMap<>();
    private final Map<String, EncodingObject> encoding = new TreeMap<>();

    private MediaTypeObject(Builder builder) {
        super(builder);
        schema = builder.schema;
        example = builder.example;
        examples.putAll(builder.examples);
        encoding.putAll(builder.encoding);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<Schema> getSchema() {
        return Optional.ofNullable(schema);
    }

    public Optional<ExampleObject> getExample() {
        return Optional.ofNullable(example);
    }

    public Map<String, ExampleObject> getExamples() {
        return examples;
    }

    public Map<String, EncodingObject> getEncoding() {
        return encoding;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember("schema", getSchema())
                .withOptionalMember("example", getExample());

        if (!examples.isEmpty()) {
            builder.withMember("examples", examples.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!encoding.isEmpty()) {
            builder.withMember("encoding", encoding.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        Map<String, Node> nodeExamples = new HashMap<>();
        for (Map.Entry<String, ExampleObject> ex : examples.entrySet()) {
            nodeExamples.put(ex.getKey(), ex.getValue().toNode());
        }
        return builder()
                .extensions(getExtensions())
                .schema(schema)
                .example(example == null ? null : example.toNode())
                .examples(nodeExamples)
                .encoding(encoding);
    }

    public static final class Builder extends Component.Builder<Builder, MediaTypeObject> {
        private Schema schema;
        private ExampleObject example;
        private final Map<String, ExampleObject> examples = new TreeMap<>();
        private final Map<String, EncodingObject> encoding = new TreeMap<>();

        private Builder() {}

        @Override
        public MediaTypeObject build() {
            return new MediaTypeObject(this);
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

        public Builder putExample(String name, ExampleObject example) {
            examples.put(name, example);
            return this;
        }

        public Builder encoding(Map<String, EncodingObject> encoding) {
            this.encoding.clear();
            this.encoding.putAll(encoding);
            return this;
        }

        public Builder putEncoding(String name, EncodingObject encodingObject) {
            encoding.put(name, encodingObject);
            return this;
        }
    }
}

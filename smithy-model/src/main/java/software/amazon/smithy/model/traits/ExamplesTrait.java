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

package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.validators.ExamplesTraitValidator;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines examples for an operation.
 *
 * @see ExamplesTraitValidator
 */
public final class ExamplesTrait extends AbstractTrait implements ToSmithyBuilder<ExamplesTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#examples");

    private final List<Example> examples;

    private ExamplesTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.examples = new ArrayList<>(builder.examples);
    }

    /**
     * @return Each example.
     */
    public List<Example> getExamples() {
        return examples;
    }

    @Override
    protected Node createNode() {
        return examples.stream().map(Example::toNode).collect(ArrayNode.collect(getSourceLocation()));
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder().sourceLocation(getSourceLocation());
        examples.forEach(builder::addExample);
        return builder;
    }

    /**
     * @return Returns a builder used to create an examples trait.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds and examples trait.
     */
    public static final class Builder extends AbstractTraitBuilder<ExamplesTrait, Builder> {
        private final List<Example> examples = new ArrayList<>();

        public Builder addExample(Example example) {
            examples.add(Objects.requireNonNull(example));
            return this;
        }

        public Builder clearExamples() {
            examples.clear();
            return this;
        }

        @Override
        public ExamplesTrait build() {
            return new ExamplesTrait(this);
        }
    }

    /**
     * A specific example.
     */
    public static final class Example implements ToNode, ToSmithyBuilder<Example> {
        private final String title;
        private final String documentation;
        private final ObjectNode input;
        private final ObjectNode output;

        private Example(Builder builder) {
            this.title = Objects.requireNonNull(builder.title, "Example title must not be null");
            this.documentation = builder.documentation;
            this.input = builder.input;
            this.output = builder.output;
        }

        /**
         * @return Returns the title.
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return Gets the optional documentation.
         */
        public Optional<String> getDocumentation() {
            return Optional.ofNullable(documentation);
        }

        /**
         * @return Gets the input object.
         */
        public ObjectNode getInput() {
            return input;
        }

        /**
         * @return Gets the output object.
         */
        public ObjectNode getOutput() {
            return output;
        }

        @Override
        public Node toNode() {
            ObjectNode.Builder builder = Node.objectNodeBuilder()
                    .withMember("title", Node.from(title))
                    .withOptionalMember("documentation", getDocumentation().map(Node::from));

            if (!input.isEmpty()) {
                builder.withMember("input", input);
            }
            if (!output.isEmpty()) {
                builder.withMember("output", output);
            }

            return builder.build();
        }

        @Override
        public Builder toBuilder() {
            return new Builder().documentation(documentation).title(title).input(input).output(output);
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder used to create {@link Example}s.
         */
        public static final class Builder implements SmithyBuilder<Example> {
            private String title;
            private String documentation;
            private ObjectNode input = Node.objectNode();
            private ObjectNode output = Node.objectNode();

            @Override
            public Example build() {
                return new Example(this);
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder documentation(String documentation) {
                this.documentation = documentation;
                return this;
            }

            public Builder input(ObjectNode input) {
                this.input = input;
                return this;
            }

            public Builder output(ObjectNode output) {
                this.output = output;
                return this;
            }
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        public ExamplesTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectArrayNode().getElements().stream()
                    .map(Node::expectObjectNode)
                    .map(Provider::exampleFromNode)
                    .forEach(builder::addExample);
            return builder.build();
        }

        private static Example exampleFromNode(ObjectNode node) {
            return Example.builder()
                    .title(node.expectStringMember("title").getValue())
                    .documentation(node.getStringMember("documentation").map(StringNode::getValue).orElse(null))
                    .input(node.getMember("input").map(Node::expectObjectNode).orElseGet(Node::objectNode))
                    .output(node.getMember("output").map(Node::expectObjectNode).orElseGet(Node::objectNode))
                    .build();
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
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
    public boolean equals(Object other) {
        if (!(other instanceof ExamplesTrait)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            ExamplesTrait trait = (ExamplesTrait) other;
            return this.examples.size() == trait.examples.size() && this.examples.containsAll(trait.examples);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(toShapeId(), examples);
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
     * Builds an examples trait.
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
        private final ErrorExample error;
        private final boolean allowConstraintErrors;

        private Example(Builder builder) {
            this.title = Objects.requireNonNull(builder.title, "Example title must not be null");
            this.documentation = builder.documentation;
            this.input = builder.input;
            this.output = builder.output;
            this.error = builder.error;
            this.allowConstraintErrors = builder.allowConstraintErrors;
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
        public Optional<ObjectNode> getOutput() {
            return Optional.ofNullable(output);
        }

        /**
         * @return Gets the error example.
         */
        public Optional<ErrorExample> getError() {
            return Optional.ofNullable(error);
        }

        /**
         * @return Returns true if input constraints errors are allowed.
         */
        public boolean getAllowConstraintErrors() {
            return allowConstraintErrors;
        }

        @Override
        public Node toNode() {
            ObjectNode.Builder builder = Node.objectNodeBuilder()
                    .withMember("title", Node.from(title))
                    .withOptionalMember("documentation", getDocumentation().map(Node::from))
                    .withOptionalMember("error", getError().map(ErrorExample::toNode));

            if (!input.isEmpty()) {
                builder.withMember("input", input);
            }
            if (this.getOutput().isPresent()) {
                builder.withMember("output", output);
            }

            if (this.allowConstraintErrors) {
                builder.withMember("allowConstraintErrors", BooleanNode.from(allowConstraintErrors));
            }

            return builder.build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Example example = (Example) o;
            return allowConstraintErrors == example.allowConstraintErrors && Objects.equals(title, example.title)
                    && Objects.equals(documentation, example.documentation)
                    && Objects.equals(input, example.input)
                    && Objects.equals(output, example.output)
                    && Objects.equals(error, example.error);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, documentation, input, output, error, allowConstraintErrors);
        }

        @Override
        public Builder toBuilder() {
            return new Builder().documentation(documentation)
                    .title(title)
                    .input(input)
                    .output(output)
                    .error(error)
                    .allowConstraintErrors(allowConstraintErrors);
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
            private ObjectNode output;
            private ErrorExample error;
            private boolean allowConstraintErrors;

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

            public Builder error(ErrorExample error) {
                this.error = error;
                return this;
            }

            public Builder allowConstraintErrors(Boolean allowConstraintErrors) {
                this.allowConstraintErrors = allowConstraintErrors;
                return this;
            }
        }
    }

    public static final class ErrorExample implements ToNode, ToSmithyBuilder<ErrorExample> {
        private final ShapeId shapeId;
        private final ObjectNode content;

        public ErrorExample(Builder builder) {
            this.shapeId = builder.shapeId;
            this.content = builder.content;
        }

        public static ErrorExample fromNode(Node node) {
            ErrorExample.Builder builder = builder();
            node.expectObjectNode()
                    .expectMember("shapeId", ShapeId::fromNode, builder::shapeId)
                    .expectObjectMember("content", builder::content);
            return builder.build();
        }

        /**
         * @return Gets the error shape id for the example.
         */
        public ShapeId getShapeId() {
            return shapeId;
        }

        /**
         * @return Gets the error object.
         */
        public ObjectNode getContent() {
            return content;
        }

        @Override
        public Node toNode() {
            return ObjectNode.objectNodeBuilder()
                    .withMember("shapeId", shapeId.toString())
                    .withMember("content", content)
                    .build();
        }

        @Override
        public Builder toBuilder() {
            return builder().content(content).shapeId(shapeId);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder implements SmithyBuilder<ErrorExample> {
            private ShapeId shapeId;
            private ObjectNode content = Node.objectNode();

            @Override
            public ErrorExample build() {
                return new ErrorExample(this);
            }

            public Builder shapeId(ShapeId shapeId) {
                this.shapeId = shapeId;
                return this;
            }

            public Builder content(ObjectNode content) {
                this.content = content;
                return this;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(shapeId, content);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other == null || other.getClass() != this.getClass()) {
                return false;
            }
            ErrorExample otherExample = (ErrorExample) other;
            return Objects.equals(shapeId, otherExample.shapeId) && Objects.equals(content, otherExample.content);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        public ExamplesTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectArrayNode().getElementsAs(Provider::exampleFromNode).forEach(builder::addExample);
            ExamplesTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }

        private static Example exampleFromNode(ObjectNode node) {
            Example.Builder builder = Example.builder();
            node.expectObjectNode()
                    .getStringMember("title", builder::title)
                    .getStringMember("documentation", builder::documentation)
                    .getObjectMember("input", builder::input)
                    .getObjectMember("output", builder::output)
                    .getMember("error", ErrorExample::fromNode, builder::error)
                    .getBooleanMember("allowConstraintErrors", builder::allowConstraintErrors);
            return builder.build();
        }
    }
}

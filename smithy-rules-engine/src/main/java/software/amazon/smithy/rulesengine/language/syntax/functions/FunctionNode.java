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

package software.amazon.smithy.rulesengine.language.syntax.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.stdlib.BooleanEquals;
import software.amazon.smithy.rulesengine.language.stdlib.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Parsed but not validated function contents containing the `fn` name and `argv`.
 */
@SmithyUnstableApi
public final class FunctionNode implements FromSourceLocation, ToNode, ToSmithyBuilder<FunctionNode> {
    private static final String ARGV = "argv";
    private static final String FN = "fn";
    private final StringNode name;

    private final SourceLocation sourceLocation;

    private final List<Expression> arguments;

    FunctionNode(Builder builder) {
        this.name = SmithyBuilder.requiredState("functionName", builder.function);
        this.sourceLocation = builder.getSourceLocation();
        this.arguments = builder.argv.copy();
    }

    /**
     * Constructs a {@link FunctionNode} for the given function name and arguments.
     *
     * @param functionName the function name.
     * @param arguments    zero or more expressions as arguments to the function.
     * @return the {@link FunctionNode}.
     */
    public static FunctionNode ofExpressions(String functionName, Expression... arguments) {
        return builder()
                .name(StringNode.from(functionName))
                .arguments(Arrays.asList(arguments))
                .build();
    }

    /**
     * Constructs a {@link FunctionNode} from the provided {@link ObjectNode}.
     *
     * @param function the node describing the function.
     * @return the {@link FunctionNode}.
     */
    public static FunctionNode fromNode(ObjectNode function) {
        List<Expression> arguments = new ArrayList<>();
        for (Node node : function.expectArrayMember(ARGV).getElements()) {
            arguments.add(Expression.fromNode(node));
        }
        return builder()
                .sourceLocation(function)
                .name(function.expectStringMember(FN))
                .arguments(arguments)
                .build();
    }

    /**
     * Returns a new builder instance.
     *
     * @return the new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new builder instance for this {@link FunctionNode}.
     *
     * @return the new builder instance.
     */
    public Builder toBuilder() {
        return builder()
                .sourceLocation(sourceLocation)
                .name(name)
                .arguments(arguments);
    }

    /**
     * Returns an expression representing this function.
     *
     * @return this function as an expression.
     */
    public Expression validate() {
        switch (name.getValue()) {
            case IsSet.ID:
                return new IsSet(this);
            case GetAttr.ID:
                return GetAttr.builder()
                        .sourceLocation(this)
                        .target(getArguments().get(0))
                        .path(getArguments().get(1).toNode().expectStringNode().getValue())
                        .build();
            case Not.ID:
                return new Not(this);
            case BooleanEquals.ID:
                return new BooleanEquals(this);
            case StringEquals.ID:
                return new StringEquals(this);
            default:
                return FunctionRegistry.forNode(this).orElseThrow(() ->
                        new RuleError(new SourceException(
                                        String.format("`%s` is not a valid function", name), name)));
        }
    }

    public String getName() {
        return name.getValue();
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder node = ObjectNode.builder();
        node.withMember(FN, name);

        ArrayNode.Builder builder = ArrayNode.builder();
        for (Expression argument : arguments) {
            builder.withValue(argument.toNode());
        }
        node.withMember(ARGV, builder.build());

        return node.build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arguments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FunctionNode functionNode = (FunctionNode) o;
        return name.equals(functionNode.name) && arguments.equals(functionNode.arguments);
    }

    public static final class Builder extends RulesComponentBuilder<Builder, FunctionNode> {
        private StringNode function;
        private final BuilderRef<List<Expression>> argv = BuilderRef.forList();

        private Builder() {
            super(SourceLocation.none());
        }

        public Builder arguments(List<Expression> argv) {
            this.argv.clear();
            this.argv.get().addAll(argv);
            return this;
        }

        @Override
        public FunctionNode build() {
            return new FunctionNode(this);
        }

        public Builder name(StringNode functionName) {
            this.function = functionName;
            return this;
        }
    }

}

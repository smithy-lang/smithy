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

package software.amazon.smithy.rulesengine.language.syntax.fn;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.stdlib.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.util.SourceLocationTrackingBuilder;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Parsed but not validated function contents containing the `fn` name and `argv`.
 */
@SmithyUnstableApi
public final class FnNode implements FromSourceLocation, ToNode {
    private static final String ARGV = "argv";
    private static final String FN = "fn";
    private final StringNode fn;

    private final SourceLocation sourceLocation;

    private final List<Expr> argv;

    FnNode(Builder builder) {
        this.fn = SmithyBuilder.requiredState("fn", builder.fn);
        this.sourceLocation = builder.getSourceLocation();
        this.argv = builder.argv.copy();
    }

    public static FnNode ofExprs(String fn, Expr... expr) {
        return new Builder(SourceLocation.none())
                .fn(StringNode.from(fn))
                .argv(Arrays.stream(expr)
                        .collect(Collectors.toList()))
                .build();
    }

    public static FnNode fromNode(ObjectNode fn) {
        return new Builder(fn)
                .fn(fn.expectStringMember(FN))
                .argv(fn
                        .expectArrayMember(ARGV)
                        .getElements()
                        .stream()
                        .map(Expr::fromNode)
                        .collect(Collectors.toList()))
                .build();
    }

    public static Builder builder(FromSourceLocation sourceLocation) {
        return new Builder(sourceLocation);
    }

    public Expr validate() {
        switch (fn.getValue()) {
            case IsSet.ID:
                return new IsSet(this);
            case GetAttr.ID:
                return GetAttr.builder(this)
                        .target(getArgv().get(0))
                        .path(getArgv().get(1).toNode().expectStringNode().getValue())
                        .build();
            case Not.ID:
                return new Not(this);
            default:
                return FunctionRegistry.getGlobalRegistry().forNode(this).orElseThrow(() ->
                        new RuleError(
                                new SourceException(
                                        String.format("`%s` is not a valid function", fn), fn)));
        }
    }

    public String getId() {
        return fn.getValue();
    }

    public List<Expr> getArgv() {
        return argv;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder node = ObjectNode.builder();
        node.withMember(FN, fn);

        ArrayNode argvNode = argv.stream().map(ToNode::toNode).collect(ArrayNode.collect());
        node.withMember(ARGV, argvNode);

        return node.build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(fn, argv);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FnNode fnNode = (FnNode) o;
        return fn.equals(fnNode.fn) && argv.equals(fnNode.argv);
    }

    public static class Builder extends SourceLocationTrackingBuilder<Builder, FnNode> {
        private StringNode fn;
        private final BuilderRef<List<Expr>> argv = BuilderRef.forList();

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder argv(List<Expr> argv) {
            this.argv.clear();
            this.argv.get().addAll(argv);
            return this;
        }

        @Override
        public FnNode build() {
            return new FnNode(this);
        }

        public Builder fn(StringNode fn) {
            this.fn = fn;
            return this;
        }
    }

}

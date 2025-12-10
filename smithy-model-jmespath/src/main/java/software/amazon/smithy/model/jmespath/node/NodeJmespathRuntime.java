/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmespath.node;

import java.util.Optional;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.EvaluationUtils;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.jmespath.evaluation.NumberType;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

public class NodeJmespathRuntime implements JmespathRuntime<Node> {

    public static final NodeJmespathRuntime INSTANCE = new NodeJmespathRuntime();

    @Override
    public RuntimeType typeOf(Node value) {
        switch (value.getType()) {
            case OBJECT:
                return RuntimeType.OBJECT;
            case ARRAY:
                return RuntimeType.ARRAY;
            case STRING:
                return RuntimeType.STRING;
            case NUMBER:
                return RuntimeType.NUMBER;
            case BOOLEAN:
                return RuntimeType.BOOLEAN;
            case NULL:
                return RuntimeType.NULL;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public Node createNull() {
        return Node.nullNode();
    }

    @Override
    public Node createBoolean(boolean b) {
        return Node.from(b);
    }

    @Override
    public boolean asBoolean(Node value) {
        try {
            return value.expectBooleanNode().getValue();
        } catch (ExpectationNotMetException e) {
            throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "Incorrect type", e);
        }
    }

    @Override
    public Node createString(String string) {
        return new StringNode(string, SourceLocation.none());
    }

    @Override
    public String asString(Node value) {
        try {
            return value.expectStringNode().getValue();
        } catch (ExpectationNotMetException e) {
            throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "Incorrect type", e);
        }
    }

    @Override
    public Node createNumber(Number value) {
        return Node.from(value);
    }

    @Override
    public NumberType numberType(Node value) {
        return EvaluationUtils.numberType(value.expectNumberNode().getValue());
    }

    @Override
    public Number asNumber(Node value) {
        try {
            return value.expectNumberNode().getValue();
        } catch (ExpectationNotMetException e) {
            throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "Incorrect type", e);
        }
    }

    @Override
    public Number length(Node value) {
        switch (value.getType()) {
            case OBJECT:
                return value.expectObjectNode().size();
            case ARRAY:
                return value.expectArrayNode().size();
            case STRING:
                return EvaluationUtils.codePointCount(value.expectStringNode().getValue());
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public Node element(Node array, Node index) {
        return array.expectArrayNode().get(index.expectNumberNode().getValue().intValue()).orElseGet(this::createNull);
    }

    @Override
    public Iterable<? extends Node> asIterable(Node value) {
        if (value.isArrayNode()) {
            return value.expectArrayNode().getElements();
        } else {
            return value.expectObjectNode().getMembers().keySet();
        }
    }

    @Override
    public ArrayBuilder<Node> arrayBuilder() {
        return new ArrayNodeBuilder();
    }

    private static final class ArrayNodeBuilder implements ArrayBuilder<Node> {
        private final ArrayNode.Builder builder = ArrayNode.builder();

        @Override
        public void add(Node value) {
            builder.withValue(value);
        }

        @Override
        public void addAll(Node value) {
            if (value.isArrayNode()) {
                builder.merge(value.expectArrayNode());
            } else {
                for (StringNode key : value.expectObjectNode().getMembers().keySet()) {
                    builder.withValue(key);
                }
            }
        }

        @Override
        public Node build() {
            return builder.build();
        }
    }

    @Override
    public Node value(Node value, Node name) {
        if (value.isObjectNode()) {
            Optional<Node> result = value.expectObjectNode().getMember(name.expectStringNode().getValue());
            return result.orElseGet(this::createNull);
        } else {
            return createNull();
        }
    }

    @Override
    public ObjectBuilder<Node> objectBuilder() {
        return new ObjectNodeBuilder();
    }

    private static final class ObjectNodeBuilder implements ObjectBuilder<Node> {
        private final ObjectNode.Builder builder = ObjectNode.builder();

        @Override
        public void put(Node key, Node value) {
            builder.withMember(key.expectStringNode(), value);
        }

        @Override
        public void putAll(Node object) {
            builder.merge(object.expectObjectNode());
        }

        @Override
        public Node build() {
            return builder.build();
        }
    }
}

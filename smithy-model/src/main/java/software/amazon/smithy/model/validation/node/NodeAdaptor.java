package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.Adaptor;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.NumberNode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class NodeAdaptor implements Adaptor<Node> {

    @Override
    public RuntimeType typeOf(Node value) {
        switch (value.getType()) {
            case OBJECT: return RuntimeType.OBJECT;
            case ARRAY: return RuntimeType.ARRAY;
            case STRING: return RuntimeType.STRING;
            case NUMBER: return RuntimeType.NUMBER;
            case BOOLEAN: return RuntimeType.BOOLEAN;
            case NULL: return RuntimeType.NULL;
            default: throw new IllegalStateException();
        }
    }

    @Override
    public Node createNull() {
        return Node.nullNode();
    }

    @Override
    public Node createBoolean(boolean b) {
        return new BooleanNode(b, SourceLocation.none());
    }

    @Override
    public boolean toBoolean(Node value) {
        return value.expectBooleanNode().getValue();
    }

    @Override
    public Node createString(String string) {
        return new StringNode(string, SourceLocation.none());
    }

    @Override
    public String toString(Node value) {
        return value.expectStringNode().getValue();
    }

    @Override
    public Node createNumber(Number value) {
        return new NumberNode(value, SourceLocation.none());
    }

    @Override
    public Number toNumber(Node value) {
        return value.expectNumberNode().getValue();
    }

    @Override
    public List<Node> toList(Node value) {
        return value.expectArrayNode().getElements();
    }

    @Override
    public ArrayBuilder<Node> arrayBuilder() {
        return new ArrayNodeBuilder();
    }

    private static class ArrayNodeBuilder implements ArrayBuilder<Node> {
        private final ArrayNode.Builder builder = ArrayNode.builder();

        @Override
        public void add(Node value) {
            builder.withValue(value);
        }

        @Override
        public void addAll(Node array) {
            builder.merge(array.expectArrayNode());
        }

        @Override
        public Node build() {
            return builder.build();
        }
    }

    @Override
    public Node getProperty(Node value, Node name) {
        Optional<Node> result = value.expectObjectNode().getMember(name.expectStringNode().getValue());
        return result.orElseGet(this::createNull);
    }

    @Override
    public Collection<? extends Node> getPropertyNames(Node value) {
        return value.expectObjectNode().getMembers().keySet();
    }

    @Override
    public ObjectBuilder<Node> objectBuilder() {
        return new ObjectNodeBuilder();
    }

    private static class ObjectNodeBuilder implements ObjectBuilder<Node> {
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

    @Override
    public int compare(Node o1, Node o2) {
        // TODO: fairly complicated, may want a default implementation based on Number?
        throw new UnsupportedOperationException();
    }
}

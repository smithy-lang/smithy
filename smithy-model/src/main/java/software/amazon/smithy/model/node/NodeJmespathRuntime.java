package software.amazon.smithy.model.node;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.NumberType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.jmespath.evaluation.EvaluationUtils;
import software.amazon.smithy.jmespath.evaluation.WrappingIterable;
import software.amazon.smithy.model.SourceLocation;

import java.util.Optional;

public class NodeJmespathRuntime implements JmespathRuntime<Node> {

    public static final NodeJmespathRuntime INSTANCE = new NodeJmespathRuntime();

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
    public boolean asBoolean(Node value) {
        return value.expectBooleanNode().getValue();
    }

    @Override
    public Node createString(String string) {
        return new StringNode(string, SourceLocation.none());
    }

    @Override
    public String asString(Node value) {
        return value.expectStringNode().getValue();
    }

    @Override
    public Node createNumber(Number value) {
        return new NumberNode(value, SourceLocation.none());
    }

    @Override
    public NumberType numberType(Node value) {
        return EvaluationUtils.numberType(value.expectNumberNode().getValue());
    }

    @Override
    public Number asNumber(Node value) {
        return value.expectNumberNode().getValue();
    }

    @Override
    public Number length(Node value) {
        switch (value.getType()) {
            case OBJECT: return value.expectObjectNode().size();
            case ARRAY: return value.expectArrayNode().size();
            case STRING: return EvaluationUtils.codePointCount(value.expectStringNode().getValue());
            default: throw new IllegalArgumentException();
        }
    }

    @Override
    public Node element(Node array, Node index) {
        return array.expectArrayNode().get(index.expectNumberNode().getValue().intValue()).orElseGet(this::createNull);
    }

    @Override
    public Iterable<Node> toIterable(Node value) {
        if (value.isArrayNode()) {
            return value.expectArrayNode().getElements();
        } else {
            return new WrappingIterable<>(x -> x, value.expectObjectNode().getMembers().keySet());
        }
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
}

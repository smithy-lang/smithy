/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node.internal;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class NodeHandler extends JsonHandler<ArrayNode.Builder, ObjectNode.Builder> {

    private Node value;

    @SmithyInternalApi
    public static Node parse(String filename, String content, boolean allowComments) {
        NodeHandler handler = new NodeHandler();
        new JsonParser(filename, handler, allowComments).parse(content);
        return handler.value;
    }

    @SmithyInternalApi
    public static String print(Node node) {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        node.accept(new NodeWriter(jsonWriter));
        return writer.toString();
    }

    @SmithyInternalApi
    public static String prettyPrint(Node node, String indentString) {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new PrettyPrintWriter(writer, indentString);
        node.accept(new NodeWriter(jsonWriter));
        return writer.toString();
    }

    @Override
    void endNull(SourceLocation location) {
        value = new NullNode(location);
    }

    @Override
    void endBoolean(boolean bool, SourceLocation location) {
        value = new BooleanNode(bool, location);
    }

    @Override
    void endString(String string, SourceLocation location) {
        value = new StringNode(string, location);
    }

    @Override
    void endNumber(String string, SourceLocation location) {
        if (string.contains("e") || string.contains("E") || string.contains(".")) {
            double doubleValue = Double.parseDouble(string);
            if (Double.isFinite(doubleValue)) {
                value = new NumberNode(doubleValue, location);
            } else {
                value = new NumberNode(new BigDecimal(string), location);
            }
        } else {
            try {
                value = new NumberNode(Long.parseLong(string), location);
            } catch (NumberFormatException e) {
                value = new NumberNode(new BigInteger(string), location);
            }

        }
    }

    @Override
    ArrayNode.Builder startArray() {
        return ArrayNode.builder();
    }

    @Override
    void endArrayValue(ArrayNode.Builder builder) {
        builder.withValue(value);
    }

    @Override
    void endArray(ArrayNode.Builder builder, SourceLocation location) {
        value = builder.sourceLocation(location).build();
    }

    @Override
    ObjectNode.Builder startObject() {
        return ObjectNode.builder();
    }

    @Override
    void endObjectValue(ObjectNode.Builder object, String name, SourceLocation keyLocation) {
        StringNode key = new StringNode(name, keyLocation);
        object.withMember(key, value);
    }

    @Override
    void endObject(ObjectNode.Builder object, SourceLocation location) {
        value = object.sourceLocation(location).build();
    }
}

/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.node.internal;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class NodeHandler extends JsonHandler<List<Node>, Map<StringNode, Node>> {

    private Node value;
    private String filename;
    private ArrayDeque<SourceLocation> locations = new ArrayDeque<>();

    NodeHandler(String filename) {
        this.filename = filename;
    }

    @SmithyInternalApi
    public static Node parse(String filename, String content, boolean allowComments) {
        try {
            NodeHandler handler = new NodeHandler(filename);
            new JsonParser(handler, allowComments).parse(content);
            return handler.value;
        } catch (ParseException e) {
            SourceLocation location = new SourceLocation(filename, e.getLocation().line, e.getLocation().column);
            throw new ModelSyntaxException("Error parsing JSON: " + e.getMessage(), location);
        }
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
    void startNull() {
        locations.push(getSourceLocation());
    }

    @Override
    void endNull() {
        value = new NullNode(locations.pop());
    }

    void startBoolean() {
        locations.push(getSourceLocation());
    }

    @Override
    void endBoolean(boolean bool) {
        value = new BooleanNode(bool, locations.pop());
    }

    void startString() {
        locations.push(getSourceLocation());
    }

    @Override
    void endString(String string) {
        value = new StringNode(string, locations.pop());
    }

    void startNumber() {
        locations.push(getSourceLocation());
    }

    @Override
    void endNumber(String string) {
        if (string.contains(".")) {
            value = new NumberNode(new BigDecimal(string), locations.pop());
        } else {
            value = new NumberNode(Long.parseLong(string), locations.pop());
        }
    }

    @Override
    List<Node> startArray() {
        locations.push(getSourceLocation());
        return new ArrayList<>();
    }

    @Override
    void endArrayValue(List<Node> array) {
        array.add(value);
    }

    @Override
    void endArray(List<Node> array) {
        value = new ArrayNode(array, locations.pop());
    }

    @Override
    Map<StringNode, Node> startObject() {
        locations.push(getSourceLocation());
        return new LinkedHashMap<>();
    }

    @Override
    void startObjectName(Map<StringNode, Node> object) {
        locations.push(getSourceLocation());
    }

    @Override
    void endObjectValue(Map<StringNode, Node> object, String name) {
        StringNode key = new StringNode(name, locations.pop());
        object.put(key, value);
    }

    @Override
    void endObject(Map<StringNode, Node> object) {
        value = new ObjectNode(object, locations.pop());
    }

    private SourceLocation getSourceLocation() {
        Location location = getLocation();
        return new SourceLocation(filename, location.line, location.column);
    }
}

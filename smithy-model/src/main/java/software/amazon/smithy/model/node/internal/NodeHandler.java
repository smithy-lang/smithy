/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node.internal;

import java.io.StringWriter;
import java.io.Writer;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Serializes {@link Node} values to JSON.
 *
 * <p>JSON parsing now lives in the loader package ({@code JsonAstReader}); this class only retains
 * the writer entry points.
 */
@SmithyInternalApi
public final class NodeHandler {

    private NodeHandler() {}

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
        prettyPrintToWriter(node, indentString, writer);
        return writer.toString();
    }

    @SmithyInternalApi
    public static void prettyPrintToWriter(Node node, String indentString, Writer writer) {
        JsonWriter jsonWriter = new PrettyPrintWriter(writer, indentString);
        node.accept(new NodeWriter(jsonWriter));
    }
}

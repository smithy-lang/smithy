/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * JSON Pointer abstraction over Smithy {@link Node} values.
 *
 * <p>A parsed JSON pointer can get a value from a Node by pointer and
 * perform JSON-patch like operations like adding a value to a specific
 * pointer target.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6902">RFC 6902</a>
 */
public final class NodePointer {

    private static final Logger LOGGER = Logger.getLogger(NodePointer.class.getName());
    private static final NodePointer EMPTY = new NodePointer("", Collections.emptyList());

    private final String originalString;
    private final List<String> parts;

    private NodePointer(String originalString, List<String> parts) {
        this.originalString = originalString;
        this.parts = parts;
    }

    /**
     * Gets an empty Node pointer.
     *
     * @return Returns a node pointer with a value of "".
     */
    public static NodePointer empty() {
        return EMPTY;
    }

    /**
     * Creates a NodePointer from a Node value.
     *
     * @param node Node value to parse.
     * @return Returns the parsed NodePointer.
     * @throws ExpectationNotMetException if the pointer cannot be parsed.
     */
    public static NodePointer fromNode(Node node) {
        try {
            String value = node.expectStringNode().getValue();
            return NodePointer.parse(value);
        } catch (RuntimeException e) {
            String message = "Expected a string containing a valid JSON pointer: " + e.getMessage();
            throw new ExpectationNotMetException(message, node);
        }
    }

    /**
     * Unescapes special JSON pointer cases.
     *
     * @param pointerPart Pointer to unescape.
     * @return Returns the unescaped pointer.
     */
    public static String unescape(String pointerPart) {
        if (!pointerPart.contains("~")) {
            return pointerPart;
        } else {
            return pointerPart.replace("~1", "/").replace("~0", "~");
        }
    }

    /**
     * Parses a JSON pointer.
     *
     * <p>A JSON pointer that starts with "#/" is treated as "/". JSON
     * pointers must start with "#/" or "/" to be parsed correctly.
     *
     * @param pointer JSON pointer to parse.
     * @return Returns the parsed pointer.
     * @throws IllegalArgumentException if the pointer does not start with slash (/).
     */
    public static NodePointer parse(String pointer) {
        return pointer.isEmpty() ? empty() : new NodePointer(pointer, parseJsonPointer(pointer));
    }

    private static List<String> parseJsonPointer(String pointer) {
        if (pointer.isEmpty()) {
            return Collections.emptyList();
        } else if (pointer.startsWith("#")) {
            // Strip a leading "#" if present.
            return parseJsonPointer(pointer.substring(1));
        } else if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException("JSON pointer must start with '/': " + pointer);
        }

        List<String> parts = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < pointer.length(); i++) {
            if (pointer.charAt(i) == '/') {
                if (start > 0) {
                    String part = pointer.substring(start, i);
                    parts.add(unescape(part));
                }
                start = i + 1;
            }
        }

        parts.add(unescape(pointer.substring(start)));
        return parts;
    }

    /**
     * Gets the parsed parts of the pointer.
     *
     * @return Returns the immutable pointer parts.
     */
    public List<String> getParts() {
        return Collections.unmodifiableList(parts);
    }

    @Override
    public String toString() {
        return originalString;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NodePointer && other.toString().equals(toString());
    }

    @Override
    public int hashCode() {
        return originalString.hashCode();
    }

    /**
     * Gets a value from a container shape at the pointer location.
     *
     * <p>When the pointer is "", then provided value is returned. When
     * the pointer is "/", a root object key value of "" is returned. When
     * an invalid property or array index is accessed, a {@link NullNode}
     * is returned. "-" can be used to access the last element of an array.
     * Any error like accessing an object key from an array or attempting
     * to access an invalid array index will return a {@code NullNode}.
     *
     * @param container Node value container to extract a value from.
     * @return Returns the extracted value or a {@link NullNode} if not found.
     */
    public Node getValue(Node container) {
        Node result = container;

        for (String part : parts) {
            if (result.asObjectNode().isPresent()) {
                result = result.expectObjectNode().getMember(part).orElse(Node.nullNode());
            } else if (result.asArrayNode().isPresent()) {
                ArrayNode array = result.expectArrayNode();
                if (part.equals("-")) {
                    return array.get(array.size() - 1).orElse(Node.nullNode());
                } else {
                    result = array.get(parseIntPart(part)).orElse(Node.nullNode());
                }
            } else {
                return Node.nullNode();
            }
        }

        return result;
    }

    /**
     * Adds or replaces a {@code value} in {@code container} at the
     * JSON pointer location.
     *
     * <p>When the JSON pointer is "", the entire document is replaced with the
     * given {@code value}. "-" can be used to access the last element of an array
     * or to add an element to the end of an array. Any error like adding
     * an object key to an array or attempting to access an invalid array
     * segment will log a warning and return the given value as-is.
     *
     * @param container Node to update.
     * @param value Value to update or replace.
     * @return Returns a representation of {@code container} with the updated value.
     */
    public Node addValue(Node container, Node value) {
        return addWithFlag(container, value, false);
    }

    /**
     * Adds or replaces a {@code value} in {@code container} at the
     * JSON pointer location.
     *
     * <p>When the JSON pointer is "", the entire document is replaced with the
     * given {@code value}. "-" can be used to access the last element of an array
     * or to add an element to the end of an array. Unlike {@link #addValue(Node, Node)},
     * attempting to add a property to a non-existent object will create a
     * new object and continue adding to the created result.
     *
     * @param container Node to update.
     * @param value Value to update or replace.
     * @return Returns a representation of {@code container} with the updated value.
     */
    public Node addWithIntermediateValues(Node container, Node value) {
        return addWithFlag(container, value, true);
    }

    private Node addWithFlag(Node container, Node value, boolean intermediate) {
        // Special case for replacing the entire document.
        if (parts.isEmpty()) {
            return value;
        } else {
            return addValue(container, value, 0, intermediate);
        }
    }

    private Node addValue(Node container, Node value, int partPosition, boolean intermediate) {
        String part = parts.get(partPosition);
        boolean isLast = partPosition == parts.size() - 1;

        if (container.isObjectNode()) {
            return addObjectMember(part, isLast, container.expectObjectNode(), value, partPosition, intermediate);
        } else if (container.isArrayNode()) {
            return addArrayMember(part, isLast, container.expectArrayNode(), value, partPosition, intermediate);
        } else {
            LOGGER.warning(() -> String.format(
                    "Attempted to add a value through JSON pointer `%s`, but segment %d targets %s",
                    toString(),
                    partPosition,
                    Node.printJson(container)));
            return container;
        }
    }

    private Node addObjectMember(
            String part,
            boolean isLast,
            ObjectNode container,
            Node value,
            int partPosition,
            boolean intermediate
    ) {
        if (isLast) {
            return container.withMember(part, value);
        } else if (container.getMember(part).isPresent()) {
            // Found the member, grab it, traverse into it, and update it.
            Node member = container.expectMember(part);
            Node updatedMember = addValue(member, value, partPosition + 1, intermediate);
            return container.withMember(part, updatedMember);
        } else if (intermediate) {
            // When creating intermediate values, generate a new object and
            // continue to traverse into it.
            Node synthesized = addValue(Node.objectNode(), value, partPosition + 1, intermediate);
            return container.withMember(part, synthesized);
        } else {
            LOGGER.warning(() -> String.format(
                    "Attempted to add a value through JSON pointer `%s`, but `%s` could not be found in %s",
                    toString(),
                    part,
                    Node.printJson(container)));
            return container;
        }
    }

    private Node addArrayMember(
            String part,
            boolean isLast,
            ArrayNode container,
            Node value,
            int partPosition,
            boolean intermediate
    ) {
        if (!isLast) {
            // "-" is a special case for the last element.
            int partInt = part.equals("-") ? container.size() - 1 : parseIntPart(part);
            if (container.get(partInt).isPresent()) {
                // Can only traverse into actual array elements.
                Node item = container.get(partInt).get();
                List<Node> list = new ArrayList<>(container.getElements());
                list.set(partInt, addValue(item, value, partPosition + 1, intermediate));
                return new ArrayNode(list, container.getSourceLocation());
            } else {
                logInvalidArrayIndex(container, partInt);
                return container;
            }
        } else if (part.equals("-")) {
            // Special case pushing to the end of the array.
            return container.withValue(value);
        } else {
            // Add the value before the given index.
            int partInt = parseIntPart(part);
            if (partInt > -1 && container.size() >= partInt) {
                List<Node> list = new ArrayList<>(container.getElements());
                list.add(partInt, value);
                return new ArrayNode(list, container.getSourceLocation());
            } else {
                // The index must exist!
                logInvalidArrayIndex(container, partInt);
                return container;
            }
        }
    }

    private void logInvalidArrayIndex(Node container, int partInt) {
        LOGGER.warning(() -> String.format(
                "Attempted to add a value through JSON pointer `%s`, but index %d could not be set in %s",
                toString(),
                partInt,
                Node.printJson(container)));
    }

    private int parseIntPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

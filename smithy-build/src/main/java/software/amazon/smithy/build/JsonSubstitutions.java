/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.util.Map;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Finds string set in a Node object string value and replaces them with a
 * corresponding Node.
 *
 * <p>Each key represents a string to search for, and each value represents
 * what to replace the string with. A value can be any type of Node, allowing
 * for strings to be changed to objects, arrays, etc. Partial string matches
 * are not currently supported.
 *
 * <p>For example, given the following values to replace:
 *
 * <p>{@code {"FOO": {"bar": "baz"}}}
 *
 * <p>and the following Node value:
 *
 * <p>{@code {"hello": "FOO", "baz": "do not replace FOO"}},
 *
 * <p>the resulting Node will become:
 *
 * <p>{@code {"hello": {"bar: "baz"}, "baz": "do not replace FOO"}}.
 *
 * <p>Notice that "do not replace FOO" was not modified because the entire
 * string did not literally match the string "FOO".
 */
public final class JsonSubstitutions {
    private final Map<String, Node> findAndReplace;

    private JsonSubstitutions(Map<String, Node> findAndReplace) {
        this.findAndReplace = MapUtils.copyOf(findAndReplace);
    }

    /**
     * Creates a substitutions instance from an ObjectNode.
     *
     * @param node ObjectNode used to create the instance.
     * @return Returns the created JsonSubstitutions.
     */
    public static JsonSubstitutions create(ObjectNode node) {
        return create(node.getStringMap());
    }

    /**
     * Creates a substitutions instance from a Map.
     *
     * @param map Map used to create the instance.
     * @return Returns the created JsonSubstitutions.
     */
    public static JsonSubstitutions create(Map<String, Node> map) {
        return new JsonSubstitutions(map);
    }

    /**
     * Replaces strings in the given node.
     *
     * @param node Node to update.
     * @return Returns the updated node.
     */
    public Node apply(Node node) {
        return node.accept(new SubstitutionVisitor()).expectObjectNode();
    }

    private final class SubstitutionVisitor extends NodeVisitor.Default<Node> {
        @Override
        protected Node getDefault(Node value) {
            return value;
        }

        @Override
        public Node arrayNode(ArrayNode node) {
            return node.getElements().stream().map(element -> element.accept(this)).collect(ArrayNode.collect());
        }

        @Override
        public Node objectNode(ObjectNode node) {
            return node.getMembers()
                    .entrySet()
                    .stream()
                    .map(entry -> Pair.of(entry.getKey(), entry.getValue().accept(this)))
                    .collect(ObjectNode.collect(Pair::getLeft, Pair::getRight));
        }

        @Override
        public Node stringNode(StringNode node) {
            return findAndReplace.getOrDefault(node.getValue(), node);
        }
    }
}

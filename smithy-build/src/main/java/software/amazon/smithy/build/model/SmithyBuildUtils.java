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

package software.amazon.smithy.build.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

final class SmithyBuildUtils {

    private SmithyBuildUtils() {}

    static String resolveImportPath(Path basePath, Node node) {
        String value = node.expectStringNode().getValue();
        return basePath == null ? value : basePath.resolve(value).toString();
    }

    static ObjectNode loadAndExpandJson(String path, String contents) {
        Node result = Node.parseJsonWithComments(contents, path);

        // No need to expand variables if they aren't used.
        if (contents.contains("${")) {
            result = result.accept(new VariableExpander());
        }

        return result.expectObjectNode();
    }

    static Path getBasePathFromSourceLocation(FromSourceLocation fromSourceLocation) {
        SourceLocation sourceLocation = fromSourceLocation.getSourceLocation();
        // Attempt to resolve a path based on the given Node.
        Path path = null;
        if (sourceLocation != SourceLocation.NONE) {
            path = Paths.get(sourceLocation.getFilename()).getParent();
        }
        if (path == null) {
            path = getCurrentWorkingDirectory();
        }
        return path;
    }

    static Path getCurrentWorkingDirectory() {
        return Paths.get(".").toAbsolutePath().normalize();
    }

    static ObjectNode expandNode(Node node) {
        return node.accept(new VariableExpander()).expectObjectNode();
    }

    /**
     * Expands ${NAME} values inside of strings to a {@code System} property
     * or an environment variable.
     */
    private static final class VariableExpander extends NodeVisitor.Default<Node> {

        private static final Pattern INLINE = Pattern.compile("(?:^|[^\\\\])\\$\\{(.+)}");
        private static final Pattern ESCAPED_INLINE = Pattern.compile("\\\\\\$");

        @Override
        protected Node getDefault(Node node) {
            return node;
        }

        @Override
        public Node arrayNode(ArrayNode node) {
            List<Node> result = new ArrayList<>(node.size());
            for (Node element : node.getElements()) {
                result.add(element.accept(this));
            }
            return new ArrayNode(result, node.getSourceLocation());
        }

        @Override
        public Node objectNode(ObjectNode node) {
            Map<StringNode, Node> result = new LinkedHashMap<>(node.size());
            for (Map.Entry<StringNode, Node> entry : node.getMembers().entrySet()) {
                result.put(entry.getKey().accept(this).expectStringNode(), entry.getValue().accept(this));
            }
            return new ObjectNode(result, node.getSourceLocation());
        }

        @Override
        public Node stringNode(StringNode node) {
            // TODO: Update this to make a single pass over the string rather than use multiple regular expressions.
            Matcher matcher = INLINE.matcher(node.getValue());
            StringBuffer builder = new StringBuffer();

            while (matcher.find()) {
                String variable = matcher.group(1);
                String replacement = expand(node.getSourceLocation(), variable);
                // INLINE over-matches to allow for escaping. If the over-matched first group does not start with
                // '${', we need to prepend the first character from that group on the replacement.
                if (!matcher.group(0).startsWith("${")) {
                    replacement = matcher.group(0).charAt(0) + replacement;
                }
                matcher.appendReplacement(builder, replacement);
            }

            matcher.appendTail(builder);

            // Remove escaped variables.
            String result = ESCAPED_INLINE.matcher(builder.toString()).replaceAll("\\$");

            return new StringNode(result, node.getSourceLocation());
        }

        private static String expand(SourceLocation sourceLocation, String variable) {
            // TODO: Add support for SMITHY_VERSION.
            if (variable.equals("SMITHY_ROOT_DIR")) {
                return SmithyBuildUtils.getCurrentWorkingDirectory().toString();
            }

            String replacement = Optional.ofNullable(System.getProperty(variable))
                    .orElseGet(() -> System.getenv(variable));

            if (replacement == null) {
                throw new SmithyBuildException(String.format(
                        "Unable to expand variable `" + variable + "` to an environment variable or system "
                        + "property: %s", sourceLocation));
            }

            return replacement;
        }
    }
}

/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Loads a {@link SmithyBuildConfig} from disk.
 */
final class ConfigLoader {

    private ConfigLoader() {}

    static SmithyBuildConfig load(Path path) {
        try {
            String content = IoUtils.readUtf8File(path);
            return load(loadWithJson(path, content).expectObjectNode());
        } catch (ModelSyntaxException e) {
            throw new SmithyBuildException(e);
        }
    }

    private static Node loadWithJson(Path path, String contents) {
        return Node.parseJsonWithComments(contents, path.toString()).accept(new VariableExpander());
    }

    private static SmithyBuildConfig load(ObjectNode node) {
        NodeMapper mapper = new NodeMapper();
        return mapper.deserialize(node, SmithyBuildConfig.class);
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
            return node.getElements().stream().map(element -> element.accept(this)).collect(ArrayNode.collect());
        }

        @Override
        public Node objectNode(ObjectNode node) {
            return node.getMembers().entrySet().stream()
                    .map(entry -> Pair.of(entry.getKey().accept(this), entry.getValue().accept(this)))
                    .collect(ObjectNode.collect(pair -> pair.getLeft().expectStringNode(), Pair::getRight));
        }

        @Override
        public Node stringNode(StringNode node) {
            Matcher matcher = INLINE.matcher(node.getValue());
            StringBuffer builder = new StringBuffer();

            while (matcher.find()) {
                String variable = matcher.group(1);
                String replacement = expand(node.getSourceLocation(), variable);
                matcher.appendReplacement(builder, replacement);
            }

            matcher.appendTail(builder);

            // Remove escaped variables.
            String result = ESCAPED_INLINE.matcher(builder.toString()).replaceAll("\\$");

            return new StringNode(result, node.getSourceLocation());
        }

        private static String expand(SourceLocation sourceLocation, String variable) {
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

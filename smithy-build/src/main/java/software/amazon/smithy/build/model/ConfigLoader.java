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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Loads a {@link SmithyBuildConfig} from disk.
 */
final class ConfigLoader {
    private static final String VERSION = "1.0";
    private static final String VERSION_KEY = "version";
    private static final String IMPORTS_KEY = "imports";
    private static final String OUTPUT_DIRECTORY_KEY = "outputDirectory";
    private static final String PROJECTIONS_KEY = "projections";
    private static final String PLUGINS_KEY = "plugins";
    private static final String ABSTRACT_KEY = "abstract";
    private static final String FILTERS_KEY = "filters";
    private static final String MAPPERS_KEY = "mappers";
    private static final String TRANSFORMS_KEY = "transforms";
    private static final String NAME_KEY = "name";
    private static final String ARGS_KEY = "args";

    private static final List<String> ROOT_KEYS = Arrays.asList(
            VERSION_KEY, IMPORTS_KEY, OUTPUT_DIRECTORY_KEY, PROJECTIONS_KEY, PLUGINS_KEY);
    private static final List<String> PROJECTION_KEYS = Arrays.asList(
            ABSTRACT_KEY, FILTERS_KEY, MAPPERS_KEY, TRANSFORMS_KEY, IMPORTS_KEY, PLUGINS_KEY);
    private static final List<String> TRANSFORM_KEYS = Arrays.asList(NAME_KEY, ARGS_KEY);

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
        SmithyBuildConfig.Builder builder = SmithyBuildConfig.builder();
        node.warnIfAdditionalProperties(ROOT_KEYS);

        node.expectStringMember(VERSION_KEY).expectOneOf(VERSION);
        builder.imports(node.getArrayMember(IMPORTS_KEY)
                .map(imports -> Node.loadArrayOfString(IMPORTS_KEY, imports))
                .orElse(Collections.emptyList()));
        node.getStringMember(OUTPUT_DIRECTORY_KEY).map(StringNode::getValue).ifPresent(builder::outputDirectory);
        builder.projections(node.getObjectMember(PROJECTIONS_KEY)
                .map(ConfigLoader::loadProjections)
                .orElse(Collections.emptyMap()));
        builder.plugins(node.getObjectMember(PLUGINS_KEY)
                .map(ConfigLoader::loadPlugins)
                .orElse(Collections.emptyMap()));
        return builder.build();
    }

    private static Map<String, ProjectionConfig> loadProjections(ObjectNode container) {
        return container.getMembers().entrySet().stream()
                .map(entry -> Pair.of(entry.getKey().getValue(), loadProjection(entry.getValue().expectObjectNode())))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private static ProjectionConfig loadProjection(ObjectNode members) {
        members.warnIfAdditionalProperties(PROJECTION_KEYS);
        ProjectionConfig.Builder builder = ProjectionConfig.builder()
                .isAbstract(members.getBooleanMemberOrDefault(ABSTRACT_KEY));
        builder.transforms(members.getArrayMember(TRANSFORMS_KEY)
                .map(ConfigLoader::loadTransforms)
                .orElse(Collections.emptyList()));
        builder.imports(members.getArrayMember(IMPORTS_KEY)
                .map(imports -> Node.loadArrayOfString(IMPORTS_KEY, imports))
                .orElse(Collections.emptyList()));
        builder.plugins(members.getObjectMember(PLUGINS_KEY)
                .map(ConfigLoader::loadPlugins)
                .orElse(Collections.emptyMap()));
        return builder.build();
    }

    private static List<TransformConfig> loadTransforms(ArrayNode node) {
        return node.getElements().stream()
                .map(element -> {
                    ObjectNode objectNode = element.expectObjectNode();
                    objectNode.warnIfAdditionalProperties(TRANSFORM_KEYS);
                    String name = objectNode.expectStringMember(NAME_KEY).getValue();
                    List<String> args = objectNode.getArrayMember(ARGS_KEY)
                            .map(argsNode -> argsNode.getElementsAs(StringNode::getValue))
                            .orElseGet(Collections::emptyList);
                    return TransformConfig.builder().name(name).args(args).build();
                })
                .collect(Collectors.toList());
    }

    private static Map<String, ObjectNode> loadPlugins(ObjectNode container) {
        return container.getMembers().entrySet().stream()
                .map(entry -> Pair.of(entry.getKey().getValue(), entry.getValue().expectObjectNode()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
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

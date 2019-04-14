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

package software.amazon.smithy.build;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
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

    SmithyBuildConfig load(Path path) {
        try {
            String content = IoUtils.readUtf8File(path);
            return load(loadWithJson(path, content).expectObjectNode(), path);
        } catch (ModelSyntaxException e) {
            throw new SmithyBuildException(e);
        }
    }

    private static Node loadWithJson(Path path, String contents) {
        return Node.parseJsonWithComments(contents, path.toString());
    }

    private SmithyBuildConfig load(ObjectNode node, Path basePath) {
        SmithyBuildConfig.Builder builder = SmithyBuildConfig.builder();
        builder.importBasePath(basePath.normalize().getParent());
        node.warnIfAdditionalProperties(ROOT_KEYS);

        node.expectMember(VERSION_KEY).expectStringNode().expectOneOf(VERSION);
        node.getMember(IMPORTS_KEY)
                .map(imports -> Node.loadArrayOfString(IMPORTS_KEY, imports))
                .ifPresent(imports -> imports.forEach(builder::addImport));
        node.getMember(OUTPUT_DIRECTORY_KEY)
                .map(Node::expectStringNode)
                .map(StringNode::getValue)
                .ifPresent(builder::outputDirectory);
        node.getMember(PROJECTIONS_KEY)
                .map(Node::expectObjectNode)
                .map(ConfigLoader::loadProjections)
                .ifPresent(projectionMap -> projectionMap.forEach(builder::addProjection));
        node.getMember(PLUGINS_KEY)
                .map(ConfigLoader::loadPlugins)
                .ifPresent(plugins -> plugins.forEach(builder::addPlugin));
        return builder.build();
    }

    private static List<Projection> loadProjections(ObjectNode container) {
        return container.getMembers().entrySet().stream()
                .map(entry -> loadProjection(entry.getKey().getValue(), entry.getValue().expectObjectNode()))
                .collect(Collectors.toList());
    }

    private static Projection loadProjection(String name, ObjectNode members) {
        members.warnIfAdditionalProperties(PROJECTION_KEYS);

        Projection.Builder builder = Projection.builder()
                .name(name)
                .isAbstract(members.getMember(ABSTRACT_KEY)
                                    .map(Node::expectBooleanNode)
                                    .map(BooleanNode::getValue)
                                    .orElse(false));
        members.getMember(TRANSFORMS_KEY)
                .map(ConfigLoader::loadTransforms)
                .orElseGet(Collections::emptyList)
                .forEach(builder::addTransform);
        members.getMember(IMPORTS_KEY)
                .map(imports -> Node.loadArrayOfString(IMPORTS_KEY, imports))
                .ifPresent(imports -> imports.forEach(builder::addImport));
        members.getMember(PLUGINS_KEY)
                .map(ConfigLoader::loadPlugins)
                .ifPresent(plugins -> plugins.forEach(builder::addPlugin));
        return builder.build();
    }

    private static List<TransformConfiguration> loadTransforms(Node node) {
        return node.expectArrayNode().getElements().stream()
                .map(element -> {
                    ObjectNode objectNode = element.expectObjectNode();
                    objectNode.warnIfAdditionalProperties(TRANSFORM_KEYS);
                    String name = objectNode.expectMember(NAME_KEY).expectStringNode().getValue();
                    List<String> args = objectNode.getArrayMember(ARGS_KEY)
                            .map(ConfigLoader::loadStringArray)
                            .orElseGet(Collections::emptyList);
                    return TransformConfiguration.builder().name(name).args(args).build();
                })
                .collect(Collectors.toList());
    }

    private static List<String> loadStringArray(Node node) {
        return node.expectArrayNode().getElements().stream()
                .map(Node::expectStringNode)
                .map(StringNode::getValue)
                .collect(Collectors.toList());
    }

    private static Map<String, ObjectNode> loadPlugins(Node container) {
        return container.expectObjectNode().getMembers().entrySet().stream()
                .map(entry -> Pair.of(entry.getKey().getValue(), entry.getValue().expectObjectNode()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }
}

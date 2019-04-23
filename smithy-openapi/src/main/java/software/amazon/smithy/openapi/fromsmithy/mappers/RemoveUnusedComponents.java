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

package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.ComponentsObject;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.SetUtils;

/**
 * Removes unused components from the OpenAPI model.
 *
 * <p>This plugin will take effect by default, but can be disabled by setting
 * "openapi.keepUnusedComponents" to true. Refs are removed in rounds until
 * a round of removals has no effect.
 *
 * <p>TODO: This plugin currently only supports the removal of schemas.
 */
public class RemoveUnusedComponents implements OpenApiMapper {
    private static final Logger LOGGER = Logger.getLogger(RemoveUnusedComponents.class.getName());

    @Override
    public byte getOrder() {
        return 64;
    }

    @Override
    public OpenApi after(Context context, OpenApi openapi) {
        if (context.getConfig().getBooleanMemberOrDefault(OpenApiConstants.OPENAPI_KEEP_UNUSED_COMPONENTS)) {
            return openapi;
        }

        OpenApi current;
        OpenApi result = openapi;

        do {
            current = result;
            result = removalRound(current);
        } while (!result.equals(current));

        return result;
    }

    private OpenApi removalRound(OpenApi openapi) {
        // Create a set of every component pointer (currently just schemas).
        String schemaPointerPrefix = OpenApiConstants.SCHEMA_COMPONENTS_POINTER + "/";
        Set<String> pointers = openapi.getComponents().getSchemas().keySet().stream()
                .map(key -> schemaPointerPrefix + key)
                .collect(Collectors.toSet());

        // Remove all found "$ref" pointers from the set, leaving only unreferenced.
        pointers.removeAll(findAllRefs(openapi.toNode().expectObjectNode()));

        if (pointers.isEmpty()) {
            return openapi;
        }

        LOGGER.info(() -> "Removing unused OpenAPI components: " + pointers);

        ComponentsObject.Builder componentsBuilder = openapi.getComponents().toBuilder();
        for (String pointer : pointers) {
            if (pointer.startsWith(schemaPointerPrefix)) {
                componentsBuilder.removeSchema(pointer.replace(schemaPointerPrefix, ""));
            } else {
                throw new UnsupportedOperationException("Unreachable statement for not yet implemented removal");
            }
        }

        return openapi.toBuilder().components(componentsBuilder.build()).build();
    }

    private Set<String> findAllRefs(ObjectNode node) {
        return node.accept(new NodeVisitor.Default<Set<String>>() {
            @Override
            protected Set<String> getDefault(Node node) {
                return SetUtils.of();
            }

            @Override
            public Set<String> arrayNode(ArrayNode node) {
                Set<String> result = new HashSet<>();
                for (Node member : node.getElements()) {
                    result.addAll(member.accept(this));
                }
                return result;
            }

            @Override
            public Set<String> objectNode(ObjectNode node) {
                Set<String> result = new HashSet<>();

                if (node.size() == 1 && node.getMember("$ref").isPresent()) {
                    node.getMember("$ref")
                            .flatMap(Node::asStringNode)
                            .map(StringNode::getValue)
                            .ifPresent(result::add);
                } else {
                    for (Node member : node.getMembers().values()) {
                        result.addAll(member.accept(this));
                    }
                }

                return result;
            }
        });
    }
}

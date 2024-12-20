/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.ComponentsObject;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Removes unused components from the OpenAPI model.
 *
 * <p>This plugin will take effect by default, but can be disabled by setting
 * "openapi.keepUnusedComponents" to true. Refs are removed in rounds until
 * a round of removals has no effect.
 *
 * <p>TODO: This plugin currently only supports the removal of schemas and security schemes.
 */
@SmithyInternalApi
public final class RemoveUnusedComponents implements OpenApiMapper {
    private static final Logger LOGGER = Logger.getLogger(RemoveUnusedComponents.class.getName());

    @Override
    public byte getOrder() {
        return 64;
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
        if (context.getConfig().getKeepUnusedComponents()) {
            return openapi;
        }

        OpenApi current;
        OpenApi result = openapi;

        do {
            current = result;
            result = removalRound(context, current);
        } while (!result.equals(current));

        result = removeUnusedSecuritySchemes(result);
        return result;
    }

    private OpenApi removalRound(Context<? extends Trait> context, OpenApi openapi) {
        // Create a set of every component pointer (currently just schemas).
        String schemaPointerPrefix = context.getConfig().getDefinitionPointer() + "/";
        Set<String> pointers = openapi.getComponents()
                .getSchemas()
                .keySet()
                .stream()
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

                if (node.getMember("$ref").isPresent()) {
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

    private OpenApi removeUnusedSecuritySchemes(OpenApi openapi) {
        // Determine which security schemes were actually used.
        Set<String> used = openapi.getSecurity()
                .stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toSet());

        for (PathItem path : openapi.getPaths().values()) {
            for (OperationObject operation : path.getOperations().values()) {
                for (Map<String, List<String>> entry : operation.getSecurity().orElse(Collections.emptyList())) {
                    used.addAll(entry.keySet());
                }
            }
        }

        // Find out if there are unused security definitions.
        Set<String> unused = new TreeSet<>(openapi.getComponents().getSecuritySchemes().keySet());
        unused.removeAll(used);

        if (unused.isEmpty()) {
            return openapi;
        }

        LOGGER.info("Removing unused OpenAPI security scheme definitions: " + unused);
        ComponentsObject.Builder componentsBuilder = openapi.getComponents().toBuilder();
        for (String unusedScheme : unused) {
            componentsBuilder.removeSecurityScheme(unusedScheme);
        }

        return openapi.toBuilder().components(componentsBuilder.build()).build();
    }
}

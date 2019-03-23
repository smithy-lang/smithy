package software.amazon.smithy.openapi.fromsmithy.plugins;

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
import software.amazon.smithy.openapi.fromsmithy.SmithyOpenApiPlugin;
import software.amazon.smithy.openapi.model.OpenApi;

/**
 * Removes unused components from the Swagger artifact.
 *
 * <p>This plugin will take effect by default, but can be disabled by setting
 * "openapi.keepUnusedComponents" to true. Refs are removed in rounds until
 * a round of removals has no effect.
 *
 * <p>TODO: This plugin currently only supports the removal of schemas.
 */
public class RemoveUnusedComponentsPlugin implements SmithyOpenApiPlugin {
    private static final Logger LOGGER = Logger.getLogger(RemoveUnusedComponentsPlugin.class.getName());

    @Override
    public OpenApi after(Context context, OpenApi openapi) {
        if (context.getConfig().getBooleanMemberOrDefault(OpenApiConstants.OPENAPI_KEEP_UNUSED_COMPONENTS)) {
            return openapi;
        }

        OpenApi current;
        var result = openapi;

        do {
            current = result;
            result = removalRound(current);
        } while (!result.equals(current));

        return result;
    }

    private OpenApi removalRound(OpenApi openapi) {
        // Create a set of every component pointer (currently just schemas).
        var schemaPointerPrefix = OpenApiConstants.SCHEMA_COMPONENTS_POINTER + "/";
        Set<String> pointers = openapi.getComponents().getSchemas().keySet().stream()
                .map(key -> schemaPointerPrefix + key)
                .collect(Collectors.toSet());

        // Remove all found "$ref" pointers from the set, leaving only unreferenced.
        pointers.removeAll(findAllRefs(openapi.toNode().expectObjectNode()));

        if (pointers.isEmpty()) {
            return openapi;
        }

        LOGGER.info(() -> "Removing unused OpenAPI components: " + pointers);

        var componentsBuilder = openapi.getComponents().toBuilder();
        for (var pointer : pointers) {
            if (pointer.startsWith(schemaPointerPrefix)) {
                componentsBuilder.removeSchema(pointer.replace(schemaPointerPrefix, ""));
            } else {
                throw new UnsupportedOperationException("Unreachable statement for not yet implemented removal");
            }
        }

        return openapi.toBuilder().components(componentsBuilder.build()).build();
    }

    private Set<String> findAllRefs(ObjectNode node) {
        return node.accept(new NodeVisitor.Default<>() {
            @Override
            protected Set<String> getDefault(Node node) {
                return Set.of();
            }

            @Override
            public Set<String> arrayNode(ArrayNode node) {
                Set<String> result = new HashSet<>();
                for (var member : node.getElements()) {
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
                    for (var member : node.getMembers().values()) {
                        result.addAll(member.accept(this));
                    }
                }

                return result;
            }
        });
    }
}

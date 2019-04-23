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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.Pair;

/**
 * Inlines primitive references to schema components and references to
 * primitive collections.
 *
 * <p>Inlining these primitive references helps to make the generated
 * OpenAPI models more idiomatic while leaving complex types as-is so that
 * they support recursive types.
 *
 * <p>A <em>primitive reference</em> is considered one of the following
 * OpenAPI types:
 *
 * <ul>
 *     <li>integer</li>
 *     <li>number</li>
 *     <li>boolean</li>
 *     <li>string</li>
 * </ul>
 *
 * <p>A <em>primitive collection</em> is an array that has an "items"
 * property that targets a primitive reference, or an object with no
 * "properties" and an "additionalProperties" reference that targets a
 * primitive type.
 *
 * <p>This conversion can be disabled by setting "openapi.disablePrimitiveInlining"
 * to {@code false}.
 */
public class InlineReferencesToPrimitiveTypes implements OpenApiMapper {
    @Override
    public byte getOrder() {
        return 110;
    }

    @Override
    public ObjectNode updateNode(Context context, OpenApi openapi, ObjectNode node) {

        if (context.getConfig().getBooleanMemberOrDefault(OpenApiConstants.DISABLE_PRIMITIVE_INLINING)) {
            return node;
        }

        // Remove primitive references in rounds until one of the rounds
        // does not modify the model. Perhaps there's a better way to do this
        // since it's rather inefficient with the number of intermediate
        // materializations of Smithy Node values.
        ObjectNode denormalized = node;
        Set<String> removedThisRound;
        Set<String> removed = new HashSet<>();

        do {
            Pair<ObjectNode, Set<String>> resultPair = denormalize(denormalized);
            denormalized = resultPair.getLeft();
            removedThisRound = resultPair.getRight();
            removed.addAll(removedThisRound);
        } while (!removedThisRound.isEmpty());

        // Now that the model has been denormalized, remove all of schema
        // components from the model that are no longer referenced. Note that
        // this differs from the RemoveUnusedComponents mapper because it
        // occurs at the serialize Node level rather than on the OpenAPI
        // object model abstractions. If these orphaned schemas were not
        // removed here, they would appear in the generated OpenAPI model.
        ObjectNode updatedComponents = denormalized
                .getObjectMember("components")
                .orElseGet(Node::objectNode);
        ObjectNode updatedSchemas = updatedComponents
                .getObjectMember("schemas")
                .orElseGet(Node::objectNode)
                .getMembers().entrySet().stream()
                .filter(entry -> !removed.contains(entry.getKey().getValue()))
                .collect(ObjectNode.collect(Map.Entry::getKey, Map.Entry::getValue));

        return denormalized.withMember("components", updatedComponents.withMember("schemas", updatedSchemas));
    }

    private static Pair<ObjectNode, Set<String>> denormalize(ObjectNode model) {
        ObjectNode schemas = model.getObjectMember("components")
                .orElseGet(Node::objectNode)
                .getObjectMember("schemas")
                .orElseGet(Node::objectNode);

        PrimitiveReferenceInliner inliner = new PrimitiveReferenceInliner(schemas);
        ObjectNode result = model.accept(inliner).expectObjectNode();
        return Pair.of(result, inliner.removed);
    }

    private static final class PrimitiveReferenceInliner extends NodeVisitor.Default<Node> {
        private final ObjectNode schemas;
        private Set<String> removed = new HashSet<>();

        private PrimitiveReferenceInliner(ObjectNode schemas) {
            this.schemas = schemas;
        }

        @Override
        public ArrayNode arrayNode(ArrayNode node) {
            return node.getElements().stream()
                    .map(elementNode -> elementNode.accept(this))
                    .collect(ArrayNode.collect());
        }

        @Override
        protected Node getDefault(Node node) {
            return node;
        }

        @Override
        public ObjectNode objectNode(ObjectNode node) {
            Optional<StringNode> ref = node.getStringMember("$ref");

            // If there's a $ref property and the referenced schema can
            // be found, the inline it if it's a primitive $ref type.
            if (ref.isPresent()) {
                String refKey = ref.get().getValue().replace("#/components/schemas/", "");
                ObjectNode target = schemas.getObjectMember(refKey).orElse(null);
                if (shouldInline(target)) {
                    removed.add(refKey);
                    return target;
                }
            }

            return node.getMembers().entrySet().stream()
                    .map(entry -> Pair.of(entry.getKey(), entry.getValue().accept(this)))
                    .collect(ObjectNode.collect(Pair::getLeft, Pair::getRight));
        }

        private static boolean shouldInline(ObjectNode target) {
            if (target == null) {
                return false;
            }

            String type = target.getStringMemberOrDefault("type", "");

            switch (type) {
                case "integer":
                case "number":
                case "boolean":
                case "string":
                    return true;
                case "array":
                    // Inline primitive lists.
                    return target.getObjectMember("items")
                            .filter(PrimitiveReferenceInliner::shouldInline)
                            .isPresent();
                case "object":
                    // Inline primitive maps.
                    return !target.getMember("properties").isPresent()
                            && target.getObjectMember("additionalProperties")
                                   .filter(PrimitiveReferenceInliner::shouldInline)
                                   .isPresent();
                default:
                    return false;
            }
        }
    }
}

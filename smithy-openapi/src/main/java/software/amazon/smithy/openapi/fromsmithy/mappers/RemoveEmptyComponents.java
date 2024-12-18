/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Removes empty key-value pairs in the "components" of a model if empty, and
 * removes the "components" key-value pair of a model if it is empty.
 */
@SmithyInternalApi
public final class RemoveEmptyComponents implements OpenApiMapper {
    private static final String COMPONENTS = "components";

    @Override
    public byte getOrder() {
        return 112;
    }

    @Override
    public ObjectNode updateNode(Context<? extends Trait> context, OpenApi openapi, ObjectNode node) {
        ObjectNode components = node.getObjectMember(COMPONENTS).orElse(null);

        if (components == null) {
            return node;
        }

        // Remove all component key value pairs that are empty objects.
        ObjectNode updatedComponents = components.getMembers()
                .entrySet()
                .stream()
                .filter(entry -> !isEmptyObject(entry.getValue()))
                .collect(ObjectNode.collect(Map.Entry::getKey, Map.Entry::getValue));

        // Remove the "components" key from the model if it's empty.
        return updatedComponents.isEmpty()
                ? node.withoutMember(COMPONENTS)
                : node.withMember(COMPONENTS, updatedComponents);
    }

    private static boolean isEmptyObject(Node node) {
        return node.asObjectNode().filter(ObjectNode::isEmpty).isPresent();
    }
}

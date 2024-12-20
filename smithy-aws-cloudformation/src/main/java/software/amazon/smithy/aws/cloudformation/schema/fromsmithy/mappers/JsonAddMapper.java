/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.aws.cloudformation.schema.CfnException;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds JSON values into the generated CloudFormation resource schemas using
 * a JSON Patch like "add" operation that also generated intermediate objects
 * as needed. Any existing property is overwritten.
 */
@SmithyInternalApi
final class JsonAddMapper implements CfnMapper {

    private static final Logger LOGGER = Logger.getLogger(JsonAddMapper.class.getName());

    @Override
    public byte getOrder() {
        // After DisableMapper in the JSON Schema conversion, with a
        // small buffer to allow for intermediate mappers.
        return 124;
    }

    @Override
    public ObjectNode updateNode(Context context, ResourceSchema resourceSchema, ObjectNode node) {
        ShapeId resourceShapeId = context.getResource().getId();
        Map<ShapeId, Map<String, Node>> add = context.getConfig().getJsonAdd();

        // Short circuit if we don't have anything to add for this resource.
        if (add.isEmpty() || !add.containsKey(resourceShapeId)) {
            return node;
        }

        // Apply the set of pointers for this resource.
        ObjectNode result = node;
        for (Map.Entry<String, Node> entry : add.get(resourceShapeId).entrySet()) {
            try {
                LOGGER.info(() -> String.format("CloudFormation `jsonAdd` for `%s`: adding `%s`",
                        resourceShapeId,
                        entry.getKey()));
                result = NodePointer.parse(entry.getKey())
                        .addWithIntermediateValues(result, entry.getValue().toNode())
                        .expectObjectNode();
            } catch (IllegalArgumentException e) {
                throw new CfnException(e.getMessage(), e);
            }
        }

        return result;
    }
}

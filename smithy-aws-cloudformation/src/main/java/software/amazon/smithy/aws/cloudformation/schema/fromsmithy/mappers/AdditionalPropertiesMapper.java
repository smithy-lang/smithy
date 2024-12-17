/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * CloudFormation issues a warning in its build tooling every time it detects
 * an "object" type in the converted JSON Schema's definitions that doesn't set
 * "additionalProperties" to "false". This mapper sets that to reduce the
 * warnings customers receive when developing resources from automatically
 * generated resource schemas.
 *
 * @see <a href="https://github.com/aws-cloudformation/cloudformation-cli/blob/8491ac54d12028a7e00295b39f102e3786e52ccf/src/rpdk/core/data_loaders.py#L286-L295">Warning in CFN CLI</a>
 */
final class AdditionalPropertiesMapper implements CfnMapper {

    @Override
    public byte getOrder() {
        // This is a desired behavior from CFN, so set it near the end
        // while still having room for other overrides if necessary.
        return 124;
    }

    @Override
    public ObjectNode updateNode(Context context, ResourceSchema resourceSchema, ObjectNode node) {
        Optional<ObjectNode> definitionsOptional = node.getObjectMember("definitions");

        // This replaces every entry in the "definitions" map and uses a
        // LinkedHashMap so that order is maintained, as entries will have
        // been sorted before we get to this mapper.
        Map<StringNode, Node> updatedNodes = new LinkedHashMap<>();
        if (definitionsOptional.isPresent()) {
            for (Map.Entry<StringNode, Node> entry : definitionsOptional.get().getMembers().entrySet()) {
                ObjectNode valueNode = entry.getValue().expectObjectNode();
                updatedNodes.put(entry.getKey(), addAdditionalProperties(valueNode));
            }
            node = node.withMember("definitions", Node.objectNode(updatedNodes));
        }
        return node;
    }

    private Node addAdditionalProperties(Node node) {
        if (!node.isObjectNode()) {
            return node;
        }
        ObjectNode valueNode = node.expectObjectNode();

        // Unions may be expressed as a "oneOf" which won't have a type.
        Optional<String> type = valueNode.getStringMember("type").map(StringNode::getValue);
        if (!type.isPresent() && valueNode.getArrayMember("oneOf").isPresent()) {
            List<Node> elements = valueNode.expectArrayMember("oneOf").getElements();
            List<Node> updatedElements = new ArrayList<>(elements.size());
            for (Node element : elements) {
                updatedElements.add(addAdditionalProperties(element));
            }
            valueNode = valueNode.withMember("oneOf", ArrayNode.fromNodes(updatedElements));
        } else if (type.isPresent() && type.get().equals("object")) {
            valueNode = valueNode.withMember("additionalProperties", false);
        }
        return valueNode;
    }
}

/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
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

                if (valueNode.expectStringMember("type").getValue().equals("object")) {
                    valueNode = valueNode.withMember("additionalProperties", false);
                }

                updatedNodes.put(entry.getKey(), valueNode);
            }
            node = node.withMember("definitions", Node.objectNode(updatedNodes));
        }
        return node;
    }
}

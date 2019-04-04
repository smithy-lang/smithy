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

package software.amazon.smithy.aws.apigateway.openapi;

import java.util.logging.Logger;
import software.amazon.smithy.aws.traits.apigateway.IntegrationTraitIndex;
import software.amazon.smithy.aws.traits.apigateway.MockIntegrationTrait;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SmithyOpenApiPlugin;
import software.amazon.smithy.openapi.model.OperationObject;

/**
 * Adds API Gateway integrations to operations.
 */
public final class AddIntegrations implements SmithyOpenApiPlugin {
    private static final String EXTENSION_NAME = "x-amazon-apigateway-integration";
    private static final Logger LOGGER = Logger.getLogger(AddIntegrations.class.getName());

    @Override
    public OperationObject updateOperation(Context context, OperationShape shape, OperationObject operation) {
        var index = context.getModel().getKnowledge(IntegrationTraitIndex.class);
        return index.getIntegrationTrait(context.getService(), shape)
                .map(trait -> {
                    var node = trait.toNode().expectObjectNode();
                    if (trait instanceof MockIntegrationTrait) {
                        node = node.withMember("type", Node.from("mock"));
                    }
                    return operation.toBuilder().putExtension(EXTENSION_NAME, node).build();
                })
                .orElseGet(() -> {
                    LOGGER.warning("No API Gateway integration trait found for " + shape.getId());
                    return operation;
                });
    }
}

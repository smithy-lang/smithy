/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.cfn;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Serializes a Smithy model to JSON AST with CloudFormation Fn::Sub
 * substitution for use as a CFN RestApi Body.
 *
 * <p>This plugin is configured using {@link SmithyCfnJsonConfig}.
 */
public final class SmithyCfnJson implements SmithyBuildPlugin {

    @Override
    public String getName() {
        return "smithy-cfn-json";
    }

    @Override
    public boolean requiresValidModel() {
        return true;
    }

    @Override
    public void execute(PluginContext context) {
        SmithyCfnJsonConfig config = new NodeMapper().deserialize(
                context.getSettings(),
                SmithyCfnJsonConfig.class);

        ShapeId serviceId = config.getService();
        if (serviceId == null) {
            throw new SmithyBuildException(
                    "smithy-cfn-json plugin requires a 'service' configuration property");
        }

        Model model = context.getModel();
        ServiceShape service = model.expectShape(serviceId, ServiceShape.class);

        ObjectNode astNode = ModelSerializer.builder().build().serialize(model);

        if (!config.getDisableCloudFormationSubstitution()) {
            astNode = astNode.accept(new CloudFormationFnSubInjector()).expectObjectNode();
        }

        context.getFileManifest()
                .writeJson(
                        service.getId().getName() + ".smithy.json",
                        astNode);
    }
}

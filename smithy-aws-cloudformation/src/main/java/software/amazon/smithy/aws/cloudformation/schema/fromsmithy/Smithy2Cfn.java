/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import java.util.Locale;
import java.util.Map;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.node.ObjectNode;

public final class Smithy2Cfn implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "cloudformation";
    }

    @Override
    public void execute(PluginContext context) {
        CfnConverter converter = CfnConverter.create();
        context.getPluginClassLoader().ifPresent(converter::classLoader);
        CfnConfig config = CfnConfig.fromNode(context.getSettings());
        converter.config(config);

        Map<String, ObjectNode> resourceNodes = converter.convertToNodes(context.getModel());
        for (Map.Entry<String, ObjectNode> resourceNode : resourceNodes.entrySet()) {
            String filename = getFileNameFromResourceType(resourceNode.getKey());
            context.getFileManifest()
                    .writeJson(
                            filename,
                            resourceNode.getValue());
        }
    }

    static String getFileNameFromResourceType(String resourceType) {
        return resourceType.toLowerCase(Locale.US).replace("::", "-") + ".json";
    }
}

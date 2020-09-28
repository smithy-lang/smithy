/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
            context.getFileManifest().writeJson(
                    filename,
                    resourceNode.getValue());
        }
    }

    static String getFileNameFromResourceType(String resourceType) {
        return resourceType.toLowerCase(Locale.US).replace("::", "-") + ".json";
    }
}

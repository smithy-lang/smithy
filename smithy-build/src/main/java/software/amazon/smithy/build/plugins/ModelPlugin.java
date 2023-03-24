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

package software.amazon.smithy.build.plugins;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;

/**
 * Writes the projected/filtered SmithyBuild model.
 */
public final class ModelPlugin implements SmithyBuildPlugin {
    private static final String NAME = "model";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(PluginContext context) {
        boolean includePrelude = context.getSettings().getBooleanMemberOrDefault("includePreludeShapes");
        context.getFileManifest().writeJson("model.json", serializeModel(context.getModel(), includePrelude));
    }

    private static Node serializeModel(Model model, boolean includePrelude) {
        return ModelSerializer.builder().includePrelude(includePrelude).build().serialize(model);
    }
}

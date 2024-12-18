/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;

/**
 * Converts Smithy to an OpenAPI model and saves it as a JSON file.
 *
 * <p>This plugin requires a setting named "service" that is the
 * Shape ID of the Smithy service shape to convert to OpenAPI.
 *
 * <p>This plugin is configured using {@link OpenApiConfig}.
 */
public final class Smithy2OpenApi implements SmithyBuildPlugin {

    @Override
    public String getName() {
        return "openapi";
    }

    @Override
    public void execute(PluginContext context) {
        OpenApiConverter converter = OpenApiConverter.create();
        context.getPluginClassLoader().ifPresent(converter::classLoader);
        OpenApiConfig config = OpenApiConfig.fromNode(context.getSettings());
        ShapeId shapeId = config.getService();
        converter.config(config);
        ObjectNode openApiNode = converter.convertToNode(context.getModel());
        context.getFileManifest().writeJson(shapeId.getName() + ".openapi.json", openApiNode);
    }
}

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

package software.amazon.smithy.openapi.fromsmithy;

import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;

/**
 * Converts Smithy to an OpenAPI model and saves it as a JSON file.
 *
 * <p>This plugin requires a setting named "service" that is the
 * Shape ID of the Smithy service shape to convert to OpenAPI.
 *
 * <p>This plugin is configured using {@link OpenApiConfig}.
 */
public final class Smithy2OpenApi implements SmithyBuildPlugin {

    private static final Logger LOGGER = Logger.getLogger(Smithy2OpenApi.class.getName());

    @Override
    public String getName() {
        return "openapi";
    }

    @Override
    public void execute(PluginContext context) {
        OpenApiConverter converter = OpenApiConverter.create();
        context.getPluginClassLoader().ifPresent(converter::classLoader);

        // Remove deprecated "openapi." prefixes from configuration settings.
        ObjectNode mapped = context.getSettings();
        for (Map.Entry<String, Node> entry : mapped.getStringMap().entrySet()) {
            if (entry.getKey().startsWith("openapi.")) {
                String expected = entry.getKey().substring(8);
                LOGGER.warning("Deprecated `openapi` configuration setting found: " + entry.getKey()
                               + ". Use " + expected + " instead");
                mapped = mapped.withoutMember(entry.getKey());
                mapped = mapped.withMember(expected, entry.getValue());
            }
        }

        NodeMapper mapper = new NodeMapper();
        OpenApiConfig config = new OpenApiConfig();
        mapper.deserializeInto(mapped, config);

        ShapeId shapeId = config.getService();

        if (shapeId == null) {
            throw new OpenApiException(getName() + " is missing required property, `service`");
        }

        ObjectNode openApiNode = converter.convertToNode(context.getModel(), shapeId);
        context.getFileManifest().writeJson(shapeId.getName() + ".openapi.json", openApiNode);
    }
}

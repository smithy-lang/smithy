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

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.jsonschema.JsonSchemaConstants;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;

/**
 * Converts Smithy to an OpenAPI model and saves it as a JSON file.
 *
 * <p>This plugin requires a setting named "service" that is the
 * Shape ID of the Smithy service shape to convert to OpenAPI.
 *
 * <p>Constants defined in {@link JsonSchemaConstants} and
 * {@link OpenApiConstants} can be provided in the settings object.
 */
public final class Smithy2OpenApi implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "openapi";
    }

    @Override
    public void execute(PluginContext context) {
        OpenApiConverter converter = OpenApiConverter.create();
        context.getSettings().getStringMap().forEach(converter::putSetting);
        context.getPluginClassLoader().ifPresent(converter::classLoader);

        var shapeId = ShapeId.from(context.getSettings()
                .expectMember(OpenApiConstants.SERVICE,
                              getName() + " required a `service` shape ID is provided in the settings.")
                .expectStringNode("`" + OpenApiConstants.SERVICE + "` must be a string value")
                .getValue());

        var openApiNode = converter.convertToNode(context.getModel(), shapeId);
        context.getFileManifest().writeJson(shapeId.getName() + ".openapi.json", openApiNode);
    }
}

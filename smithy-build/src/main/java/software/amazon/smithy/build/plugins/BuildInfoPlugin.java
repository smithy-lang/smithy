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

import java.util.Map;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Writes a build info file for each projection. This file contains the
 * projection name, the filters, mappers, and validation events encountered
 * while building the model.
 *
 * <p>This plugin is invoked regardless of whether or not the model contains
 * error or unsuppressed validation events.
 *
 * <p>This plugin is only invoked if the projection and original model are
 * configured on the provided PluginContext.
 *
 * <p>TODO: define the schema of this build artifact.
 */
public final class BuildInfoPlugin implements SmithyBuildPlugin {
    private static final String BUILD_INFO_VERSION = "1.0";
    private static final String NAME = "build-info";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean requiresValidModel() {
        return false;
    }

    @Override
    public void execute(PluginContext context) {
        if (context.getOriginalModel().isPresent() && context.getProjection().isPresent()) {
            context.getFileManifest().writeJson("smithy-build-info.json", serializeBuildInfo(context));
        }
    }

    private static Node serializeBuildInfo(PluginContext context) {
        return Node.objectNodeBuilder()
                .withMember("version", Node.from(BUILD_INFO_VERSION))
                .withMember("projectionName", Node.from(context.getProjectionName()))
                .withMember("projection", context.getProjection().get().toNode())
                .withMember("validationEvents", context.getEvents().stream()
                        .map(ValidationEvent::toNode)
                        .collect(ArrayNode.collect()))
                .withMember("traitNames", findTraitNames(context.getModel()))
                .withMember("traitDefNames", context.getModel().getTraitShapes().stream()
                        .map(Shape::getId)
                        .map(ShapeId::toString)
                        .sorted()
                        .map(Node::from)
                        .collect(ArrayNode.collect()))
                .withMember("serviceShapeIds", findShapeIds(context.getModel(), ServiceShape.class))
                .withMember("operationShapeIds", findShapeIds(context.getModel(), OperationShape.class))
                .withMember("resourceShapeIds", findShapeIds(context.getModel(), ResourceShape.class))
                .withMember("metadata", context.getModel().getMetadata().entrySet().stream()
                        .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)))
                .build();
    }

    private static Node findTraitNames(Model model) {
        return model.shapes()
                .flatMap(shape -> shape.getAllTraits().keySet().stream())
                .map(ShapeId::toString)
                .distinct()
                .sorted()
                .map(Node::from)
                .collect(ArrayNode.collect());
    }

    private static <T extends Shape> Node findShapeIds(Model model, Class<T> clazz) {
        return model.shapes(clazz)
                .map(Shape::getId)
                .map(Object::toString)
                .sorted()
                .map(Node::from)
                .collect(ArrayNode.collect());
    }
}

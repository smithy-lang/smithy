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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.Projection;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;
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
            context.getFileManifest().writeJson("smithy-build-info.json", serializeBuildInfo(
                    context.getModel(), context.getProjection().get(), context.getEvents()));
        }
    }

    private static Node serializeBuildInfo(Model model, Projection projection, List<ValidationEvent> events) {
        return Node.objectNodeBuilder()
                .withMember("version", Node.from(BUILD_INFO_VERSION))
                .withMember("smithyVersion", Node.from(model.getSmithyVersion()))
                .withMember("projection", projection.toNode())
                .withMember("validationEvents", events.stream()
                        .map(ValidationEvent::toNode)
                        .collect(ArrayNode.collect()))
                .withMember("traitNames", findTraitNames(model))
                .withMember("traitDefNames", model.getTraitDefinitions().stream()
                        .map(TraitDefinition::getFullyQualifiedName)
                        .map(Node::from)
                        .collect(ArrayNode.collect()))
                .withMember("serviceShapeIds", findShapeIds(model, ServiceShape.class))
                .withMember("operationShapeIds", findShapeIds(model, OperationShape.class))
                .withMember("resourceShapeIds", findShapeIds(model, ResourceShape.class))
                .withMember("metadata", model.getMetadata().entrySet().stream()
                        .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)))
                .build();
    }

    private static Node findTraitNames(Model model) {
        return model.getShapeIndex().shapes()
                .flatMap(shape -> shape.getAllTraits().keySet().stream())
                .collect(Collectors.toSet())
                .stream()
                .map(Node::from)
                .collect(ArrayNode.collect());
    }

    private static <T extends Shape> Node findShapeIds(Model model, Class<T> clazz) {
        return model.getShapeIndex().shapes(clazz)
                .map(Shape::getId)
                .map(Object::toString)
                .map(Node::from)
                .collect(ArrayNode.collect());
    }
}

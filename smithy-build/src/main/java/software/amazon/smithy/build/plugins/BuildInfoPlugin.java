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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitDefinition;

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
 */
public final class BuildInfoPlugin implements SmithyBuildPlugin {
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
        BuildInfo info = new BuildInfo();
        info.setProjectionName(context.getProjectionName());
        info.setProjection(context.getProjection().orElse(null));
        info.setValidationEvents(context.getEvents());
        info.setTraitNames(findTraitNames(context.getModel()));
        info.setTraitDefNames(getTraitShapeIds(context.getModel()));
        info.setServiceShapeIds(findShapeIds(context.getModel(), ServiceShape.class));
        info.setOperationShapeIds(findShapeIds(context.getModel(), OperationShape.class));
        info.setResourceShapeIds(findShapeIds(context.getModel(), ResourceShape.class));
        info.setMetadata(context.getModel().getMetadata());
        NodeMapper mapper = new NodeMapper();
        return mapper.serialize(info);
    }

    private static List<ShapeId> getTraitShapeIds(Model model) {
        Set<Shape> traits = model.getShapesWithTrait(TraitDefinition.class);
        List<ShapeId> result = new ArrayList<>(traits.size());
        for (Shape traitShape : traits) {
            result.add(traitShape.getId());
        }
        Collections.sort(result);
        return result;
    }

    private static List<ShapeId> findTraitNames(Model model) {
        List<ShapeId> applied = new ArrayList<>(model.getAppliedTraits());
        Collections.sort(applied);
        return applied;
    }

    private static <T extends Shape> List<ShapeId> findShapeIds(Model model, Class<T> clazz) {
        Set<T> shapes = model.toSet(clazz);
        List<ShapeId> result = new ArrayList<>(shapes.size());
        for (Shape s : shapes) {
            result.add(s.getId());
        }
        Collections.sort(result);
        return result;
    }
}

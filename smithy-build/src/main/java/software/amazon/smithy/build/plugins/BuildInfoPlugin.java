/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
    public static final String BUILD_INFO_PATH = "smithy-build-info.json";
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
            context.getFileManifest().writeJson(BUILD_INFO_PATH, serializeBuildInfo(context));
        }
    }

    private static Node serializeBuildInfo(PluginContext context) {
        BuildInfo info = new BuildInfo();
        info.setProjectionName(context.getProjectionName());
        info.setProjection(context.getProjection().orElse(null));
        info.setValidationEvents(context.getEvents());
        Set<ShapeId> traitIds = getTraitShapeIds(context.getModel());
        info.setTraitNames(findTraitNames(context.getModel(), traitIds));
        info.setTraitDefNames(new ArrayList<>(traitIds));
        info.setServiceShapeIds(findShapeIds(context.getModel(), ServiceShape.class));
        info.setOperationShapeIds(findShapeIds(context.getModel(), OperationShape.class));
        info.setResourceShapeIds(findShapeIds(context.getModel(), ResourceShape.class));
        info.setMetadata(context.getModel().getMetadata());
        NodeMapper mapper = new NodeMapper();
        return mapper.serialize(info);
    }

    private static Set<ShapeId> getTraitShapeIds(Model model) {
        // Will only get traits that are defined in the model (no synthetic traits)
        Set<Shape> traits = model.getShapesWithTrait(TraitDefinition.class);

        Set<ShapeId> result = new TreeSet<>();
        for (Shape traitShape : traits) {
            result.add(traitShape.getId());
        }

        return result;
    }

    private static List<ShapeId> findTraitNames(Model model, Set<ShapeId> traitIds) {

        List<ShapeId> applied = new ArrayList<>(model.getAppliedTraits());

        List<ShapeId> traitNames = new ArrayList<>();
        for (ShapeId shapeId : applied) {
            if (traitIds.contains(shapeId)) {
                traitNames.add(shapeId);
            }
        }
        Collections.sort(traitNames);

        return traitNames;
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

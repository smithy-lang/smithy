/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.build.transforms.ExcludeTraitsByTag;
import software.amazon.smithy.codegen.core.directed.CodegenDirector;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates Java code implementations of traits from a Smithy model.
 */
@SmithyInternalApi
public final class TraitCodegenPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(TraitCodegenPlugin.class.getName());
    private static final String NAME = "trait-codegen";
    private static final ShapeId SYNTHETIC_SERVICE_ID = ShapeId.from("smithy.synthetic#TraitService");

    private final CodegenDirector<TraitCodegenWriter, TraitCodegenIntegration, TraitCodegenContext,
            TraitCodegenSettings> runner = new CodegenDirector<>();


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(PluginContext context) {
        runner.directedCodegen(new TraitCodegenDirectedCodegen());
        runner.integrationClass(TraitCodegenIntegration.class);
        runner.fileManifest(context.getFileManifest());
        TraitCodegenSettings settings = TraitCodegenSettings.fromNode(context.getSettings());
        runner.model(transform(context.getModel(), settings));
        runner.settings(settings);
        runner.service(SYNTHETIC_SERVICE_ID);
        runner.performDefaultCodegenTransforms();
        LOGGER.info("Plugin Initialized. Executing Trait Codegen Plugin.");
        runner.run();
        LOGGER.info("Trait Codegen plugin executed successfully.");
    }

    private static Model transform(Model model, TraitCodegenSettings settings) {
        return addSyntheticService(removeTraitsWithTags(model, settings.excludeTags()));
    }

    private static Model removeTraitsWithTags(Model model, List<String> tags) {
        if (tags.isEmpty()) {
            return model;
        }
        return new ExcludeTraitsByTag().transform(TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("tags", Node.fromStrings(tags)))
                .build());
    }

    private static Model addSyntheticService(Model model) {
        // Find all trait definition shapes excluding traits in the prelude.
        Set<Shape> toGenerate = model.getShapesWithTrait(TraitDefinition.class).stream()
                .filter(shape -> !Prelude.isPreludeShape(shape))
                .collect(Collectors.toSet());

        Set<Shape> shapesToAdd = new HashSet<>();

        // Create a synthetic service builder to add operations to
        ServiceShape.Builder serviceBuilder = ServiceShape.builder().id(SYNTHETIC_SERVICE_ID);

        // Create a synthetic operation for each trait and add to the synthetic service
        for (Shape traitShape : toGenerate) {
            OperationShape op = OperationShape.builder()
                    .id(traitShape.getId().toString() + "SyntheticOperation")
                    .input(traitShape.toShapeId())
                    .build();
            shapesToAdd.add(op);
            serviceBuilder.addOperation(op.toShapeId());
        }
        shapesToAdd.add(serviceBuilder.build());

        return ModelTransformer.create().replaceShapes(model, shapesToAdd);
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.build.transforms.ExcludeTraitsByTag;
import software.amazon.smithy.codegen.core.directed.CodegenDirector;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.traitcodegen.integrations.TraitCodegenIntegration;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

public class TraitCodegenPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(TraitCodegenPlugin.class.getName());
    private static final String NAME = "trait-codegen";
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
        TraitCodegenSettings settings = TraitCodegenSettings.from(context.getSettings());
        Model model = removeTraitsWithTags(context.getModel(), settings.excludeTags());
        runner.model(SyntheticTraitServiceTransformer.transform(model));
        runner.settings(settings);
        runner.service(SyntheticTraitServiceTransformer.SYNTHETIC_SERVICE_ID);
        runner.performDefaultCodegenTransforms();
        LOGGER.info("Plugin Initialized. Executing Trait Codegen Plugin.");
        runner.run();
        LOGGER.info("Trait Codegen plugin executed successfully.");
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
}

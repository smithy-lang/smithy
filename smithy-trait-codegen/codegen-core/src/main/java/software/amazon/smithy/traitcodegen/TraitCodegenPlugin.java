/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.util.logging.Logger;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates Java code implementations of traits from a Smithy model.
 */
@SmithyInternalApi
public final class TraitCodegenPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(TraitCodegenPlugin.class.getName());
    private static final String NAME = "trait-codegen";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(PluginContext context) {
        LOGGER.info("Running trait codegen plugin...");
        TraitCodegen traitCodegen = TraitCodegen.fromPluginContext(context);
        traitCodegen.initialize();
        traitCodegen.run();
        LOGGER.info("Trait codegen plugin execution completed.");
    }
}

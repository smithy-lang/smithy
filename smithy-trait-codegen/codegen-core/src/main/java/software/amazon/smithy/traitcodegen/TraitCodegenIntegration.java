/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Allows additional functionality to be added into the trait codegen generator.
 *
 * <p>{@code TraitCodegenIntegration}'s are loaded as a Java SPI. To make your integration
 * discoverable, add a file to {@code META-INF/services} named
 * {@code software.amazon.smithy.traitcodegen.TraitCodegenIntegration} where each line is
 * the fully-qualified class name of your integrations. Several tools, such as
 * {@code AutoService}, can do this for you.
 */
public interface TraitCodegenIntegration extends SmithyIntegration<TraitCodegenSettings, TraitCodegenWriter,
        TraitCodegenContext> {

    /**
     * Updates the {@link TraitGeneratorProvider} used when to generate trait code.
     *
     * <p>This can be used to override the implementation of a trait generator
     * based on the shape type or base on meta-traits.
     *
     * <p>By default, this method will return the given {@code TraitGeneratorProvider}
     * as-is.
     *
     * @param context The context of the code generator.
     * @return The decorated {@code TraitGeneratorProvider}.
     */
    default TraitGeneratorProvider decorateGeneratorProvider(TraitCodegenContext context,
                                                             TraitGeneratorProvider provider
    ) {
        return provider;
    }
}

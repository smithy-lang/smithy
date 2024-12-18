/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations;

import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Allows additional functionality to be added into the trait codegen generator.
 *
 * <p>{@code TraitCodegenIntegration}'s are loaded as a Java SPI. To make your integration
 * discoverable, add a file to {@code META-INF/services} named
 * {@code software.amazon.smithy.traitcodegen.integrations.TraitCodegenIntegration} where each line is
 * the fully-qualified class name of your integrations. Several tools, such as
 * {@code AutoService}, can do this for you.
 */
public interface TraitCodegenIntegration extends SmithyIntegration<TraitCodegenSettings, TraitCodegenWriter,
        TraitCodegenContext> {}

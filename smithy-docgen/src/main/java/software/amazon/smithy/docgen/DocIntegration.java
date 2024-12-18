/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import java.util.List;
import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Allows integrating additional functionality into the documentation generator.
 *
 * <p>{@code DocIntegration}s are loaded as a Java SPI. To make your integration
 * discoverable, add a file to {@code META-INF/services} named
 * {@code software.amazon.smithy.docgen.DocIntegration} where each line is
 * the fully-qualified class name of your integrations. Several tools, such as
 * {@code AutoService}, can do this for you.
 */
@SmithyUnstableApi
public interface DocIntegration extends SmithyIntegration<DocSettings, DocWriter, DocGenerationContext> {

    /**
     * Adds {@link DocFormat}s to the list of supported formats.
     *
     * <p>When resolving the format implementation, the first format found with a
     * matching name will be used. Use {@link #priority} to adjust which integration
     * is seen first.
     *
     * @param settings The documentation generation settings.
     * @return A list of formats to add.
     */
    default List<DocFormat> docFormats(DocSettings settings) {
        return List.of();
    }
}

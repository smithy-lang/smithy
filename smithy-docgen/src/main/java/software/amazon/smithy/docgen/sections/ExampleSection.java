/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.generators.OperationGenerator;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.ExamplesTrait.Example;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Generates a single operation example as defined by the {@code examples} trait.
 *
 * <p>This modifies the contents of a single example. To modify the entire example
 * section, use {@link ExamplesSection} instead.
 *
 * @param context The context used to generate documentation.
 * @param operation The operation whose examples are being documented.
 * @param example The example that will be documented.
 *
 * @see ExamplesSection
 * @see OperationGenerator
 */
@SmithyUnstableApi
public record ExampleSection(
        DocGenerationContext context,
        OperationShape operation,
        Example example) implements CodeSection {}

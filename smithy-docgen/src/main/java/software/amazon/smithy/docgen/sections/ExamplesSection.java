/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import java.util.List;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.generators.OperationGenerator;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.ExamplesTrait.Example;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Generates the documentation for an operation's examples as defined by the
 * {@code example} trait.
 *
 * <p>This controls all the examples for an operation. To modify a single example, use
 * {@link ExampleSection} instead.
 *
 * @param context The context used to generate documentation.
 * @param operation The operation whose examples are being documented.
 * @param examples The list of examples that will be documented.
 *
 * @see ExampleSection
 * @see OperationGenerator
 */
@SmithyUnstableApi
public record ExamplesSection(
        DocGenerationContext context,
        OperationShape operation,
        List<Example> examples) implements CodeSection {}

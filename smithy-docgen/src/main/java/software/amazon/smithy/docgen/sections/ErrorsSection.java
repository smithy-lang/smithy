/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.DocIntegration;
import software.amazon.smithy.docgen.generators.OperationGenerator;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains a listing of all the errors that an operation might throw, or errors common
 * to a resource or service.
 *
 * <p>To simply add errors to a shape, instead use
 * {@link DocIntegration#preprocessModel} to add
 * them to the shape directly.
 *
 * @param context The context used to generate documentation.
 * @param shape The shape whose errors are being documented.
 *
 * @see OperationGenerator
 */
@SmithyUnstableApi
public record ErrorsSection(DocGenerationContext context, Shape shape) implements CodeSection {}

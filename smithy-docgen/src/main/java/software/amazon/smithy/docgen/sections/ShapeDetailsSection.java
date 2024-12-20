/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Enables injecting details immediately after a shape's modeled documentation.
 *
 * @param context The context used to generate documentation.
 * @param shape The shape whose documentation is being generated.
 *
 * @see ShapeSection to modify the shape's entire documentation.
 * @see ShapeSubheadingSection to inject docs before modeled documentation.
 */
@SmithyUnstableApi
public record ShapeDetailsSection(DocGenerationContext context, Shape shape) implements CodeSection {}

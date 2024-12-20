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
 * Enables injecting details immediately before a shape's modeled documentation.
 *
 * @param context The context used to generate documentation.
 * @param shape The shape whose documentation is being generated.
 *
 * @see ShapeSection to modify the shape's entire documentation.
 * @see ShapeDetailsSection to inject docs after modeled documentation.
 */
@SmithyUnstableApi
public record ShapeSubheadingSection(DocGenerationContext context, Shape shape) implements CodeSection {}

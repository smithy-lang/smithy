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
 * Generates documentation for shapes.
 *
 * @param context The context used to generate documentation.
 * @param shape   The shape whose documentation is being generated.
 *
 * @see ShapeDetailsSection to insert details after the shape's modeled docs.
 * @see ShapeMembersSection to modify the listing of the shape's members (if any).
 * @see MemberSection to modify the documentation for an individual shape member.
 */
@SmithyUnstableApi
public record ShapeSection(DocGenerationContext context, Shape shape) implements CodeSection {}

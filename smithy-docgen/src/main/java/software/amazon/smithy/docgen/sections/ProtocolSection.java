/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A section that contains protocol-specific information for a specific protocol
 * for a given shape.
 *
 * @param context The context used to generate documentation.
 * @param shape The shape to add protocol information to.
 * @param protocol The shape id of the protocol being documented.
 *
 * @see ProtocolsSection to make changes to all protocols and how they're displayed.
 * @see ShapeSection to make non-protocol-specific changes to a shape's docs.
 */
@SmithyUnstableApi
public record ProtocolSection(
        DocGenerationContext context,
        Shape shape,
        ShapeId protocol) implements CodeSection {}

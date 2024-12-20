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
 * A section that contains all protocol-specific information for the given shape for
 * all protocols.
 *
 * <p>Each individual protocol has its own {@link ProtocolSection}. If the service has
 * more than one protocol, these sections will be in tabs.
 *
 * @param context The context used to generate documentation.
 * @param shape The shape to add protocol information to.
 *
 * @see ProtocolSection to make additions to a particular protocol's section.
 * @see ShapeSection to make non-protocol-specific changes to a shape's docs.
 */
@SmithyUnstableApi
public record ProtocolsSection(
        DocGenerationContext context,
        Shape shape) implements CodeSection {}

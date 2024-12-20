/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Enables modifying or overwriting the documentation for a member.
 *
 * @param context The context used to generate documentation.
 * @param member The member whose documentation is being generated.
 *
 * @see ShapeMembersSection to modify the listing of all members of the shape.
 * @see ShapeSubheadingSection to add context immediately before the member's docs.
 */
@SmithyUnstableApi
public record MemberSection(DocGenerationContext context, MemberShape member) implements CodeSection {}

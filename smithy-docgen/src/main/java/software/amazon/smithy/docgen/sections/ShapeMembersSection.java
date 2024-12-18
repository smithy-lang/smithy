/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import java.util.Collection;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.generators.MemberGenerator.MemberListingType;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Generates a listing of a shape's members.
 *
 * @param context The context used to generate documentation.
 * @param shape The shape whose member documentation is being generated.
 * @param members The members being generated.
 * @param listingType The type of the listing.
 *
 * @see MemberSection to modify the documentation for an individual member.
 */
@SmithyUnstableApi
public record ShapeMembersSection(
        DocGenerationContext context,
        Shape shape,
        Collection<MemberShape> members,
        MemberListingType listingType) implements CodeSection {}

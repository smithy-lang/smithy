/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.core.generators;

import java.util.function.BiConsumer;
import software.amazon.smithy.docgen.core.DocGenerationContext;
import software.amazon.smithy.docgen.core.DocSymbolProvider;
import software.amazon.smithy.docgen.core.generators.MemberGenerator.MemberListingType;
import software.amazon.smithy.docgen.core.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.core.sections.ShapeSection;
import software.amazon.smithy.docgen.core.sections.ShapeSubheadingSection;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates documentation for shapes with members.
 *
 * <p>The output of this can be customized in a number of ways. To add details to
 * or re-write particular sections, register an interceptor with
 * {@link software.amazon.smithy.docgen.core.DocIntegration#interceptors}. The following
 * sections are guaranteed to be present:
 *
 * <ul>
 *     <li>{@link ShapeSubheadingSection}: Enables adding additional details that are
 *     inserted right after the shape's heading, before modeled docs.
 *
 *     <li>{@link ShapeDetailsSection}: Enables adding additional details that are inserted
 *     directly after the shape's modeled documentation.
 *
 *     <li>{@link ShapeSection}: Enables re-writing or overwriting the entire page,
 *     including changes made in other sections.
 *
 *     <li>{@link software.amazon.smithy.docgen.core.sections.ProtocolSection} Enables adding
 *     traits that are specific to a particular protocol. This section will only be present if
 *     there are protocol traits applied to the service. If there are multiple protocol traits,
 *     this section will appear once per protocol. This section will also appear for each member.
 *
 *     <li>{@link software.amazon.smithy.docgen.core.sections.ProtocolsSection} Enables
 *     modifying the tab group containing all the protocol traits for all the protocols. This
 *     section will also appear for each member.
 * </ul>
 *
 * Additionally, if the shape has members the following sections will also be present:
 *
 * <ul>
 *     <li>{@link software.amazon.smithy.docgen.core.sections.MemberSection}: enables
 *     modifying documentation for an individual shape member.
 *
 *     <li>{@link software.amazon.smithy.docgen.core.sections.ShapeMembersSection}:
 *     enables modifying the documentation for all of the shape's members.
 * </ul>
 *
 * <p>To change the intermediate format (e.g. from markdown to restructured text),
 * a new {@link software.amazon.smithy.docgen.core.DocFormat} needs to be introduced
 * via {@link software.amazon.smithy.docgen.core.DocIntegration#docFormats}.
 *
 * @see MemberGenerator for more details on how member documentation is generated.
 */
@SmithyInternalApi
public final class StructuredShapeGenerator implements BiConsumer<Shape, MemberListingType> {

    private final DocGenerationContext context;

    /**
     * Constructs a StructuredShapeGenerator.
     *
     * @param context The context used to generate documentation.
     */
    public StructuredShapeGenerator(DocGenerationContext context) {
        this.context = context;
    }

    @Override
    public void accept(Shape shape, MemberListingType listingType) {
        var symbol = context.symbolProvider().toSymbol(shape);
        context.writerDelegator().useShapeWriter(shape, writer -> {
            writer.pushState(new ShapeSection(context, shape));
            symbol.getProperty(DocSymbolProvider.LINK_ID_PROPERTY, String.class).ifPresent(writer::writeAnchor);
            writer.openHeading(symbol.getName());

            writer.injectSection(new ShapeSubheadingSection(context, shape));
            writer.writeShapeDocs(shape, context.model());
            writer.injectSection(new ShapeDetailsSection(context, shape));
            GeneratorUtils.writeProtocolsSection(context, writer, shape);

            new MemberGenerator(context, writer, shape, listingType).run();

            writer.closeHeading();
            writer.popState();
        });
    }
}

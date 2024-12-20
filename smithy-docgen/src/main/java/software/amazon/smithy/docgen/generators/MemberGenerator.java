/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.generators;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.docgen.DocFormat;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.DocIntegration;
import software.amazon.smithy.docgen.DocSymbolProvider;
import software.amazon.smithy.docgen.sections.MemberSection;
import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.sections.ProtocolsSection;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.sections.ShapeMembersSection;
import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates documentation for shape members.
 *
 * <p>The output of this can be customized in a number of ways. To add details to
 * or re-write particular sections, register an interceptor with
 * {@link DocIntegration#interceptors}. The following
 * sections will be present:
 *
 * <ul>
 *     <li>{@link MemberSection}: Enables re-writing the documentation for specific members.
 *
 *     <li>{@link ShapeMembersSection}: Enables re-writing or overwriting the entire list
 *     of members, including changes made in other sections.
 *
 *     <li>{@link ProtocolSection} Enables adding
 *     traits that are specific to a particular protocol. This section will only be present if
 *     there are protocol traits applied to the service. If there are multiple protocol traits,
 *     this section will appear once per protocol.
 *
 *     <li>{@link ProtocolsSection} Enables
 *     modifying the tab group containing all the protocol traits for all the protocols.
 * </ul>
 *
 * <p>To change the intermediate format (e.g. from markdown to restructured text),
 * a new {@link DocFormat} needs to be introduced
 * via {@link DocIntegration#docFormats}.
 */
@SmithyUnstableApi
public final class MemberGenerator implements Runnable {

    private final DocGenerationContext context;
    private final Shape shape;
    private final MemberListingType listingType;
    private final DocWriter writer;

    /**
     * Constructs a MemberGenerator.
     *
     * @param context The context used to generate documentation.
     * @param writer The writer to write to.
     * @param shape The shape whose members are being generated.
     * @param listingType The type of listing being generated.
     */
    public MemberGenerator(
            DocGenerationContext context,
            DocWriter writer,
            Shape shape,
            MemberListingType listingType
    ) {
        this.context = context;
        this.writer = writer;
        this.shape = shape;
        this.listingType = listingType;
    }

    @Override
    public void run() {
        var members = getMembers();
        writer.pushState(new ShapeMembersSection(context, shape, members, listingType));
        var parentSymbol = context.symbolProvider().toSymbol(shape);
        if (!members.isEmpty()) {
            parentSymbol.getProperty(DocSymbolProvider.LINK_ID_PROPERTY, String.class).ifPresent(linkId -> {
                writer.writeAnchor(linkId + "-" + listingType.getLinkIdSuffix());
            });
            writer.openHeading(listingType.getTitle());
            writer.openDefinitionList();
            for (MemberShape member : members) {
                writer.pushState(new MemberSection(context, member));

                var symbol = context.symbolProvider().toSymbol(member);
                var target = context.model().expectShape(member.getTarget());

                var typeWriter = writer.consumer(w -> target.accept(new MemberTypeVisitor(w, context, member)));
                writer.openDefinitionListItem(w -> w.writeInline("$L ($C)", symbol.getName(), typeWriter));

                writer.injectSection(new ShapeSubheadingSection(context, member));
                writer.writeShapeDocs(member, context.model());
                writer.injectSection(new ShapeDetailsSection(context, member));
                GeneratorUtils.writeProtocolsSection(context, writer, member);
                writer.closeDefinitionListItem();
                writer.popState();
            }
            writer.closeDefinitionList();
            writer.closeHeading();
        }
        writer.popState();
    }

    private Collection<MemberShape> getMembers() {
        return switch (listingType) {
            case INPUT -> context.model()
                    .expectShape(shape.asOperationShape().get().getInputShape())
                    .getAllMembers()
                    .values();
            case OUTPUT -> context.model()
                    .expectShape(shape.asOperationShape().get().getOutputShape())
                    .getAllMembers()
                    .values();
            case RESOURCE_IDENTIFIERS -> synthesizeResourceMembers(shape.asResourceShape().get().getIdentifiers());
            case RESOURCE_PROPERTIES -> synthesizeResourceMembers(shape.asResourceShape().get().getProperties());
            default -> shape.getAllMembers().values();
        };
    }

    // Resource identifiers and properties aren't actually members, but they're close
    // enough that we can treat them like they are for the purposes of the doc generator.
    private List<MemberShape> synthesizeResourceMembers(Map<String, ShapeId> properties) {
        return properties.entrySet()
                .stream()
                .map(entry -> MemberShape.builder()
                        .id(shape.getId().withMember(entry.getKey()))
                        .target(entry.getValue())
                        .build())
                .toList();
    }

    /**
     * The type of listing. This controls the heading title and anchor id for the section.
     */
    public enum MemberListingType {
        /**
         * Indicates the listing is for normal shape members.
         */
        MEMBERS("Members"),

        /**
         * Indicates the listing is for an operation's input members.
         */
        INPUT("Request Members"),

        /**
         * Indicates the listing is for an operation's output members.
         */
        OUTPUT("Response Members"),

        /**
         * Indicates the listing is for enums, intEnums, or unions, which each only
         * allow one of their members to be selected.
         */
        OPTIONS("Options"),

        /**
         * Indicates the listing is for a resource's identifiers.
         */
        RESOURCE_IDENTIFIERS("Identifiers"),

        /**
         * Indicates the listing is for a resource's modeled properties.
         */
        RESOURCE_PROPERTIES("Properties");

        private final String title;
        private final String linkIdSuffix;

        MemberListingType(String title) {
            this.title = title;
            this.linkIdSuffix = title.toLowerCase(Locale.ENGLISH).strip().replaceAll("\\s+", "-");
        }

        /**
         * @return returns the heading title that should be used for the listing.
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return returns the suffix that will be applied to the parent shape's link
         *         id to form this member listing's link id.
         */
        public String getLinkIdSuffix() {
            return linkIdSuffix;
        }
    }

    private static class MemberTypeVisitor extends ShapeVisitor.Default<Void> {

        private final DocWriter writer;
        private final DocGenerationContext context;
        private final MemberShape member;

        MemberTypeVisitor(DocWriter writer, DocGenerationContext context, MemberShape member) {
            this.writer = writer;
            this.context = context;
            this.member = member;
        }

        @Override
        protected Void getDefault(Shape shape) {
            throw new CodegenException(String.format(
                    "Unexpected member %s of type %s",
                    shape.getId(),
                    shape.getType()));
        }

        @Override
        public Void blobShape(BlobShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void booleanShape(BooleanShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void byteShape(ByteShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void shortShape(ShortShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void longShape(LongShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void floatShape(FloatShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void bigIntegerShape(BigIntegerShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void bigDecimalShape(BigDecimalShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            writeShapeName(shape);
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            writer.writeInline("List\\<");
            context.model().expectShape(shape.getMember().getTarget()).accept(this);
            writer.writeInline("\\>");
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writer.writeInline("Map\\<");
            context.model().expectShape(shape.getKey().getTarget()).accept(this);
            writer.writeInline(", ");
            context.model().expectShape(shape.getValue().getTarget()).accept(this);
            writer.writeInline("\\>");
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            if (member.hasTrait(EnumValueTrait.class)) {
                var trait = member.expectTrait(EnumValueTrait.class);
                if (trait.getIntValue().isPresent()) {
                    writer.writeInline("$`", trait.expectIntValue());
                } else {
                    writer.writeInline("$`", trait.expectStringValue());
                }
            } else {
                writeShapeName(shape);
            }
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            writeShapeName(shape);
            return null;
        }

        private void writeShapeName(Shape shape) {
            var symbol = context.symbolProvider().toSymbol(shape);

            if (StringUtils.isNotBlank(symbol.getDefinitionFile())) {
                writer.writeInline("$R", symbol);
            } else {
                // If the symbol doesn't have a definition file, it can't be linked.
                // If it can't be linked to, then the actual name of the shape
                // doesn't matter and would only serve as a confusing dead reference
                // if displayed in the docs. Instead we just use the shape type name,
                // which should be more clear in almost every case. A SymbolReference
                // is passed along rather than writing a literal string so that
                // implementations can do something with the source symbol if
                // necessary.
                var reference = SymbolReference.builder()
                        .symbol(symbol)
                        .alias(StringUtils.capitalize(shape.getType().name().toLowerCase(Locale.ENGLISH)))
                        .build();
                writer.writeInline("$R", reference);
            }
        }
    }
}

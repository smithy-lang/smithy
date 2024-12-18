/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.generators;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.DocSymbolProvider;
import software.amazon.smithy.docgen.sections.BoundOperationSection;
import software.amazon.smithy.docgen.sections.BoundOperationsSection;
import software.amazon.smithy.docgen.sections.BoundResourceSection;
import software.amazon.smithy.docgen.sections.BoundResourcesSection;
import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.sections.ProtocolsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.ListType;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Provides common generation methods for services and resources.
 */
@SmithyInternalApi
final class GeneratorUtils {
    private GeneratorUtils() {}

    static void generateOperationListing(
            DocGenerationContext context,
            DocWriter writer,
            EntityShape shape,
            List<OperationShape> operations
    ) {
        writer.pushState(new BoundOperationsSection(context, shape, operations));

        if (operations.isEmpty()) {
            writer.popState();
            return;
        }

        var parentLinkId = context.symbolProvider()
                .toSymbol(shape)
                .expectProperty(DocSymbolProvider.LINK_ID_PROPERTY, String.class);
        writer.openHeading("Operations", parentLinkId + "-operations");
        writer.openList(ListType.UNORDERED);

        for (var operation : operations) {
            writer.pushState(new BoundOperationSection(context, shape, operation));
            writeListingElement(context, writer, operation);
            writer.popState();
        }

        writer.closeList(ListType.UNORDERED);
        writer.closeHeading();
        writer.popState();
    }

    static void generateResourceListing(
            DocGenerationContext context,
            DocWriter writer,
            EntityShape shape,
            List<ResourceShape> resources
    ) {
        writer.pushState(new BoundResourcesSection(context, shape, resources));

        if (resources.isEmpty()) {
            writer.popState();
            return;
        }

        var parentLinkId = context.symbolProvider()
                .toSymbol(shape)
                .expectProperty(DocSymbolProvider.LINK_ID_PROPERTY, String.class);
        var heading = shape.isServiceShape() ? "Resources" : "Sub-Resources";
        writer.openHeading(heading, parentLinkId + "-" + heading.toLowerCase(Locale.ENGLISH));
        writer.openList(ListType.UNORDERED);

        for (var resource : resources) {
            writer.pushState(new BoundResourceSection(context, shape, resource));
            writeListingElement(context, writer, resource);
            writer.popState();
        }

        writer.closeList(ListType.UNORDERED);
        writer.closeHeading();
        writer.popState();
    }

    private static void writeListingElement(DocGenerationContext context, DocWriter writer, Shape shape) {
        writer.openListItem(ListType.UNORDERED);
        var symbol = context.symbolProvider().toSymbol(shape);
        writer.writeInline("$R: ", symbol).writeShapeDocs(shape, context.model());
        writer.closeListItem(ListType.UNORDERED);
    }

    static void writeProtocolsSection(DocGenerationContext context, DocWriter writer, Shape shape) {
        var protocols = ServiceIndex.of(context.model()).getProtocols(context.settings().service()).keySet();
        if (protocols.isEmpty()) {
            return;
        }
        writer.pushState(new ProtocolsSection(context, shape));

        AtomicReference<String> tabGroupContents = new AtomicReference<>();
        var tabGroup = capture(writer, tabGroupWriter -> {
            tabGroupWriter.openTabGroup();
            tabGroupContents.set(capture(tabGroupWriter, w -> {
                for (var protocol : protocols) {
                    writeProtocolSection(context, w, shape, protocol);
                }
            }));
            tabGroupWriter.closeTabGroup();
        });

        if (StringUtils.isBlank(tabGroupContents.get())) {
            // The extra newline is needed because the section intercepting logic actually adds one
            // by virtue of calling write instead of writeInline
            writer.unwrite("$L\n", tabGroup);
        }

        writer.popState();
    }

    private static void writeProtocolSection(
            DocGenerationContext context,
            DocWriter writer,
            Shape shape,
            ShapeId protocol
    ) {
        var protocolSymbol = context.symbolProvider().toSymbol(context.model().expectShape(protocol));

        AtomicReference<String> tabContents = new AtomicReference<>();
        var tab = capture(writer, tabWriter -> {
            tabWriter.openTab(protocolSymbol.getName());
            tabContents.set(capture(tabWriter,
                    w2 -> tabWriter.injectSection(
                            new ProtocolSection(context, shape, protocol))));
            tabWriter.closeTab();
        });

        if (StringUtils.isBlank(tabContents.get())) {
            // The extra newline is needed because the section intercepting logic actually adds one
            // by virtue of calling write instead of writeInline
            writer.unwrite("$L\n", tab);
        }
    }

    /**
     * Captures and returns what is written by the given consumer.
     *
     * @param writer The writer to capture from.
     * @param consumer A consumer that writes text to be captured.
     * @return Returns what was written by the consumer.
     */
    private static String capture(DocWriter writer, Consumer<DocWriter> consumer) {
        var recorder = new RecordingInterceptor();
        writer.pushState(new CapturingSection()).onSection(recorder);
        consumer.accept(writer);
        writer.popState();
        return recorder.getContents();
    }

    private record CapturingSection() implements CodeSection {}

    /**
     * Records what was written to the section previously and writes it back.
     */
    private static final class RecordingInterceptor implements CodeInterceptor<CapturingSection, DocWriter> {
        private String contents = null;

        public String getContents() {
            return contents;
        }

        @Override
        public Class<CapturingSection> sectionType() {
            return CapturingSection.class;
        }

        @Override
        public void write(DocWriter writer, String previousText, CapturingSection section) {
            contents = previousText;
            writer.writeWithNoFormatting(previousText);
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.generators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import software.amazon.smithy.codegen.core.directed.GenerateOperationDirective;
import software.amazon.smithy.codegen.core.docs.Snippet;
import software.amazon.smithy.codegen.core.docs.SnippetFile;
import software.amazon.smithy.docgen.DocFormat;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.DocIntegration;
import software.amazon.smithy.docgen.DocSettings;
import software.amazon.smithy.docgen.DocSymbolProvider;
import software.amazon.smithy.docgen.generators.MemberGenerator.MemberListingType;
import software.amazon.smithy.docgen.sections.ErrorsSection;
import software.amazon.smithy.docgen.sections.ExampleSection;
import software.amazon.smithy.docgen.sections.ExamplesSection;
import software.amazon.smithy.docgen.sections.MemberSection;
import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.sections.ProtocolsSection;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.sections.ShapeMembersSection;
import software.amazon.smithy.docgen.sections.ShapeSection;
import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.ListType;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.traits.ExamplesTrait.Example;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates documentation for operations.
 *
 * <p>The output of this can be customized in a number of ways. To add details to
 * or re-write particular sections, register an interceptor with
 * {@link DocIntegration#interceptors}. The following
 * sections are guaranteed to be present:
 *
 * <ul>
 *     <li>{@link ShapeSubheadingSection}: Enables adding additional details that are
 *     inserted right after the shape's heading, before modeled docs.
 *
 *     <li>{@link ShapeDetailsSection}: Enables adding additional details that are inserted
 *     directly after the shape's modeled documentation.
 *
 *     <li>{@link ShapeSection}: Three versions of this section will appear on the page.
 *     The first is for the operation shape itself, which enables re-writing or adding
 *     details to the entire page. The other two are for the input and output shapes,
 *     which enable modifying the documentation for just the input and output sections.
 *
 *     <li>{@link ErrorsSection}: This section will contain a listing of all the errors
 *     the operation might return. If a synthetic error needs to be applied to an
 *     operation, it is better to simply add it to the shape with
 *     {@link DocIntegration#preprocessModel}.
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
 * Additionally, if the operation's input or output shapes have members the following
 * sections will also be present:
 *
 * <ul>
 *     <li>{@link MemberSection}: enables
 *     modifying documentation for an individual input or output member.
 *
 *     <li>{@link ShapeMembersSection}:
 *     Two versions of this section will appear on the page, one for the operation's
 *     input shape members and one for the operation's output shape members. These
 *     enable re-writing or editing those sections.
 * </ul>
 *
 * If the {@code examples} trait has been applied to the operation, it will also have
 * the following sections:
 *
 * <ul>
 *     <li>{@link ExamplesSection}: enables modifying the entire examples section.
 *
 *     <li>{@link ExampleSection}: enables modifying a singular example, including the
 *     snippets in every discovered language.
 * </ul>
 *
 * <p>To change the intermediate format (e.g. from markdown to restructured text),
 * a new {@link DocFormat} needs to be introduced
 * via {@link DocIntegration#docFormats}.
 *
 * @see MemberGenerator for more details on how member documentation is generated.
 */
@SmithyInternalApi
public final class OperationGenerator
        implements Consumer<GenerateOperationDirective<DocGenerationContext, DocSettings>> {
    @Override
    public void accept(GenerateOperationDirective<DocGenerationContext, DocSettings> directive) {
        var operation = directive.shape();
        var context = directive.context();
        var symbol = directive.symbolProvider().toSymbol(operation);
        context.writerDelegator().useShapeWriter(directive.shape(), writer -> {
            writer.pushState(new ShapeSection(context, operation));
            var linkId = symbol.expectProperty(DocSymbolProvider.LINK_ID_PROPERTY, String.class);
            writer.openHeading(symbol.getName(), linkId);
            writer.injectSection(new ShapeSubheadingSection(context, operation));
            writer.writeShapeDocs(operation, directive.model());
            writer.injectSection(new ShapeDetailsSection(context, operation));
            GeneratorUtils.writeProtocolsSection(context, writer, operation);

            new MemberGenerator(context, writer, operation, MemberListingType.INPUT).run();
            new MemberGenerator(context, writer, operation, MemberListingType.OUTPUT).run();

            writeErrors(context, writer, directive.service(), operation, linkId);

            var examples = operation.getTrait(ExamplesTrait.class).map(ExamplesTrait::getExamples).orElse(List.of());
            writeExamples(context, writer, operation, examples, linkId);

            writer.closeHeading();
            writer.popState();
        });
    }

    private void writeErrors(
            DocGenerationContext context,
            DocWriter writer,
            ServiceShape service,
            OperationShape operation,
            String linkId
    ) {
        var errors = operation.getErrors(service);
        writer.pushState(new ErrorsSection(context, operation));
        if (!errors.isEmpty()) {
            writer.openHeading("Errors", linkId + "-errors");
            writer.write("This operation may return any of the following errors:");
            writer.openList(ListType.UNORDERED);
            for (var error : errors) {
                var errorShape = context.model().expectShape(error);
                writer.openListItem(ListType.UNORDERED);
                writer.writeInline("$R: ", context.symbolProvider().toSymbol(errorShape));
                writer.writeShapeDocs(errorShape, context.model());
                writer.closeListItem(ListType.UNORDERED);
            }
            writer.closeList(ListType.UNORDERED);
            writer.closeHeading();
        }
        writer.popState();
    }

    private void writeExamples(
            DocGenerationContext context,
            DocWriter writer,
            OperationShape operation,
            List<Example> examples,
            String operationLinkId
    ) {
        writer.pushState(new ExamplesSection(context, operation, examples));
        var snippets = getExampleSnippets(context, operation, examples);
        if (snippets.isEmpty()) {
            writer.popState();
            return;
        }

        writer.openHeading("Examples", operationLinkId + "-examples");
        for (var example : examples) {
            var exampleSnippets = snippets.get(example.getTitle());
            if (exampleSnippets == null) {
                continue;
            }

            writer.pushState(new ExampleSection(context, operation, example));
            var linkIdSuffix = example.getTitle().toLowerCase(Locale.ENGLISH).strip().replaceAll("\\s+", "-");
            writer.openHeading(example.getTitle(), operationLinkId + "-" + linkIdSuffix);
            example.getDocumentation().ifPresent(writer::writeCommonMark);

            writer.openTabGroup();

            for (Snippet snippet : exampleSnippets) {
                if (snippet.getFiles().size() == 1) {
                    var file = snippet.getFiles().get(0);
                    writer.openCodeTab(snippet.getTitle(), file.getLanguage());
                    writer.write(file.getContent());
                    writer.closeCodeTab();
                } else {
                    writer.openTab(snippet.getTitle());
                    writer.openTabGroup();
                    for (SnippetFile file : snippet.getFiles()) {
                        writer.openCodeTab(file.getFilename(), file.getLanguage());
                        writer.write(file.getContent());
                        writer.closeCodeTab();
                    }
                    writer.closeTabGroup();
                    writer.closeCodeTab();
                }
            }

            writer.closeTabGroup();
            writer.closeHeading();
            writer.popState();
        }
        writer.closeHeading();
        writer.popState();
    }

    private Map<String, List<Snippet>> getExampleSnippets(
            DocGenerationContext context,
            OperationShape operation,
            List<Example> examples
    ) {
        var exampleMap = examples.stream().collect(Collectors.groupingBy(Example::getTitle));
        var snippets = context.snippetConfig().getShapeSnippets(context.settings().service(), operation.getId());
        var resolvedSnippets = new LinkedHashMap<String, List<Snippet>>();
        for (Snippet snippet : snippets) {

            // Ensure that the service has the protocol if one is expected for the snippet.
            // This could be a bad snippet, or it could be that the protocol was filtered
            // out.
            if (snippet.getProtocol().isPresent()) {
                var service = context.model().expectShape(context.settings().service());
                if (!service.hasTrait(snippet.getProtocol().get())) {
                    continue;
                }
            }

            // Ensure that the snippet represent an example that's actually in the model.
            var example = exampleMap.get(snippet.getTargetId());
            if (example == null) {
                continue;
            }
            resolvedSnippets.computeIfAbsent(snippet.getTargetId(), id -> new ArrayList<>()).add(snippet);
        }
        return resolvedSnippets;
    }
}

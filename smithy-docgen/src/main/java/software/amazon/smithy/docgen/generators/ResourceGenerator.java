/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.generators;

import java.util.HashSet;
import java.util.Locale;
import java.util.function.BiConsumer;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.docgen.DocFormat;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.DocIntegration;
import software.amazon.smithy.docgen.DocSymbolProvider;
import software.amazon.smithy.docgen.generators.MemberGenerator.MemberListingType;
import software.amazon.smithy.docgen.sections.BoundOperationSection;
import software.amazon.smithy.docgen.sections.BoundOperationsSection;
import software.amazon.smithy.docgen.sections.BoundResourceSection;
import software.amazon.smithy.docgen.sections.BoundResourcesSection;
import software.amazon.smithy.docgen.sections.LifecycleOperationSection;
import software.amazon.smithy.docgen.sections.LifecycleOperationSection.LifecycleType;
import software.amazon.smithy.docgen.sections.LifecycleSection;
import software.amazon.smithy.docgen.sections.MemberSection;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.sections.ShapeMembersSection;
import software.amazon.smithy.docgen.sections.ShapeSection;
import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates documentation for the resources.
 *
 * <p>The output of this can be customized in a number of ways. To add details to
 * or re-write particular sections, register an interceptor with
 * {@link DocIntegration#interceptors}. The following
 * sections are guaranteed to be present:
 *
 * <ul>
 *     <li>{@link ShapeSection}: Enables re-writing or overwriting the entire page,
 *     including changes made in other sections.
 *
 *     <li>{@link ShapeSubheadingSection}: Enables adding additional details that are
 *     inserted right after the resource's heading, before modeled docs.
 *
 *     <li>{@link ShapeDetailsSection}: Enables adding in additional details that are
 *     inserted after the resource's modeled documentation.
 *
 *     <li>{@link ShapeMembersSection}:
 *     Two versions of this section will appear on the page, one for the resource's
 *     identifiers and one for the resource's properties. These enable re-writing or
 *     editing those entire sections.
 *
 *     <li>{@link MemberSection}: enables
 *     modifying documentation for an individual resource property or identifier.
 *
 *     <li>{@link BoundOperationsSection}:
 *     enables modifying the listing of operations directly bound to the resource. This
 *     does not include any operations transitively bound to the resource through
 *     sub-resources or lifecycle operations.
 *
 *     <li>{@link BoundOperationSection}:
 *     enables modifying the listing of an individual operation bound to the resource.
 *     This does not include any operations transitively bound to the resource through
 *     sub-resources or lifecycle operations. This section will only be present if
 *     there are operations directly bound to the resource.
 *
 *     <li>{@link LifecycleSection}: enables modifying the listing of operations bound
 *     as one of the resource's lifecycle operations.
 *
 *     <li>{@link LifecycleOperationSection}: enables modifying the listing of an
 *     individual resource lifecycle operation. This section will only be present if
 *     the resource has bound lifecycle operations.
 *
 *     <li>{@link BoundResourcesSection}:
 *     enables modifying the listing of sub-resources directly bound to the resource.
 *
 *     <li>{@link BoundResourceSection}:
 *     enables modifying the listing of an individual sub-resource directly bound to
 *     the resource. This section will only be present if the resource has any
 *     sub-resources.
 * </ul>
 *
 * <p>To change the intermediate format (e.g. from markdown to restructured text),
 * a new {@link DocFormat} needs to be introduced
 * via {@link DocIntegration#docFormats}.
 *
 * <p>To change the filename or title, implement
 * {@link DocIntegration#decorateSymbolProvider}
 * and modify the generated symbol's definition file. See
 * {@link DocSymbolProvider} for details on other
 * symbol-driven configuration options.
 *
 * @see <a href="https://smithy.io/2.0/spec/service-types.html#resource">
 *     Smithy resource shape docs.</a>
 */
@SmithyInternalApi
public final class ResourceGenerator implements BiConsumer<DocGenerationContext, ResourceShape> {
    @Override
    public void accept(DocGenerationContext context, ResourceShape resource) {
        var symbol = context.symbolProvider().toSymbol(resource);

        context.writerDelegator().useShapeWriter(resource, writer -> {
            writer.pushState(new ShapeSection(context, resource));
            var linkId = symbol.expectProperty(DocSymbolProvider.LINK_ID_PROPERTY, String.class);
            writer.openHeading(symbol.getName(), linkId);
            writer.injectSection(new ShapeSubheadingSection(context, resource));
            writer.writeShapeDocs(resource, context.model());
            writer.injectSection(new ShapeDetailsSection(context, resource));
            GeneratorUtils.writeProtocolsSection(context, writer, resource);

            new MemberGenerator(context, writer, resource, MemberListingType.RESOURCE_IDENTIFIERS).run();
            new MemberGenerator(context, writer, resource, MemberListingType.RESOURCE_PROPERTIES).run();

            var subResources = resource.getResources()
                    .stream()
                    .sorted()
                    .map(id -> context.model().expectShape(id, ResourceShape.class))
                    .toList();
            GeneratorUtils.generateResourceListing(context, writer, resource, subResources);

            generateLifecycleDocs(context, writer, resource);

            var operationIds = new HashSet<>(resource.getOperations());
            operationIds.addAll(resource.getCollectionOperations());
            var operations = operationIds.stream()
                    .sorted()
                    .map(id -> context.model().expectShape(id, OperationShape.class))
                    .toList();
            GeneratorUtils.generateOperationListing(context, writer, resource, operations);

            writer.closeHeading();
            writer.popState();
        });
    }

    private void generateLifecycleDocs(DocGenerationContext context, DocWriter writer, ResourceShape resource) {
        writer.pushState(new LifecycleSection(context, resource));
        if (!hasLifecycleBindings(resource)) {
            writer.popState();
            return;
        }
        var linkId = context.symbolProvider()
                .toSymbol(resource)
                .expectProperty(DocSymbolProvider.LINK_ID_PROPERTY, String.class);
        writer.openHeading("Lifecycle Operations", linkId + "-lifecycle-operations");
        writer.openDefinitionList();

        if (resource.getPut().isPresent()) {
            var put = context.model().expectShape(resource.getPut().get(), OperationShape.class);
            writeLifecycleListing(context, writer, resource, put, LifecycleType.PUT);
        }

        if (resource.getCreate().isPresent()) {
            var create = context.model().expectShape(resource.getCreate().get(), OperationShape.class);
            writeLifecycleListing(context, writer, resource, create, LifecycleType.CREATE);
        }

        if (resource.getRead().isPresent()) {
            var read = context.model().expectShape(resource.getRead().get(), OperationShape.class);
            writeLifecycleListing(context, writer, resource, read, LifecycleType.READ);
        }

        if (resource.getUpdate().isPresent()) {
            var update = context.model().expectShape(resource.getUpdate().get(), OperationShape.class);
            writeLifecycleListing(context, writer, resource, update, LifecycleType.UPDATE);
        }

        if (resource.getDelete().isPresent()) {
            var delete = context.model().expectShape(resource.getDelete().get(), OperationShape.class);
            writeLifecycleListing(context, writer, resource, delete, LifecycleType.DELETE);
        }

        if (resource.getList().isPresent()) {
            var list = context.model().expectShape(resource.getList().get(), OperationShape.class);
            writeLifecycleListing(context, writer, resource, list, LifecycleType.LIST);
        }

        writer.closeDefinitionList();
        writer.closeHeading();
        writer.popState();
    }

    private boolean hasLifecycleBindings(ResourceShape resource) {
        return resource.getPut().isPresent()
                || resource.getCreate().isPresent()
                || resource.getRead().isPresent()
                || resource.getUpdate().isPresent()
                || resource.getDelete().isPresent()
                || resource.getList().isPresent();
    }

    private void writeLifecycleListing(
            DocGenerationContext context,
            DocWriter writer,
            ResourceShape resource,
            OperationShape operation,
            LifecycleType lifecycleType
    ) {
        writer.pushState(new LifecycleOperationSection(context, resource, operation, lifecycleType));
        var lifecycleName = StringUtils.capitalize(lifecycleType.name().toLowerCase(Locale.ENGLISH));
        var operationName = context.symbolProvider().toSymbol(operation).getName();
        var reference = SymbolReference.builder()
                .symbol(context.symbolProvider().toSymbol(operation))
                .alias(String.format("%s (%s)", lifecycleName, operationName))
                .build();
        writer.openDefinitionListItem(w -> w.writeInline("$R", reference));
        writer.writeShapeDocs(operation, context.model());
        writer.closeDefinitionListItem();
        writer.popState();
    }
}

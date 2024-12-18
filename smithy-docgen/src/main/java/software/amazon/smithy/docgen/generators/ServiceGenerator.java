/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.generators;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.directed.GenerateServiceDirective;
import software.amazon.smithy.docgen.DocFormat;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.DocIntegration;
import software.amazon.smithy.docgen.DocSettings;
import software.amazon.smithy.docgen.DocSymbolProvider;
import software.amazon.smithy.docgen.DocgenUtils;
import software.amazon.smithy.docgen.sections.AuthSection;
import software.amazon.smithy.docgen.sections.BoundOperationSection;
import software.amazon.smithy.docgen.sections.BoundOperationsSection;
import software.amazon.smithy.docgen.sections.BoundResourceSection;
import software.amazon.smithy.docgen.sections.BoundResourcesSection;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.sections.ShapeSection;
import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex.AuthSchemeMode;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.synthetic.NoAuthTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates top-level documentation for the service.
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
 *     inserted right after the shape's heading, before modeled docs.
 *
 *     <li>{@link ShapeDetailsSection}: Enables adding in additional details that are
 *     inserted after the service's modeled documentation.
 *
 *     <li>{@link BoundOperationsSection}:
 *     enables modifying the listing of operations transitively bound to the service,
 *     which includes operations bound to resources.
 *
 *     <li>{@link BoundOperationSection}:
 *     enables modifying the listing of an individual operation transitively bound to
 *     the service.
 *
 *     <li>{@link BoundResourcesSection}:
 *     enables modifying the listing of resources directly bound to the service.
 *
 *     <li>{@link BoundResourceSection}:
 *     enables modifying the listing of an individual resource directly bound to
 *     the service.
 *
 *     <li>{@link AuthSection} enables modifying the documentation for the different
 *     auth schemes available on the service. This section will not be present if
 *     the service has no auth traits.
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
 * @see <a href="https://smithy.io/2.0/spec/service-types.html#service">
 *     Smithy service shape docs.</a>
 */
@SmithyInternalApi
public final class ServiceGenerator implements Consumer<GenerateServiceDirective<DocGenerationContext, DocSettings>> {

    @Override
    public void accept(GenerateServiceDirective<DocGenerationContext, DocSettings> directive) {
        var service = directive.service();
        var context = directive.context();
        var serviceSymbol = directive.symbolProvider().toSymbol(service);

        directive.context().writerDelegator().useShapeWriter(service, writer -> {
            writer.pushState(new ShapeSection(context, service));
            writer.openHeading(serviceSymbol.getName());
            writer.injectSection(new ShapeSubheadingSection(context, service));
            writer.writeShapeDocs(service, directive.model());
            writer.injectSection(new ShapeDetailsSection(context, service));

            var topDownIndex = TopDownIndex.of(context.model());

            // TODO: topographically sort resources
            var resources = topDownIndex.getContainedResources(service).stream().sorted().toList();
            GeneratorUtils.generateResourceListing(context, writer, service, resources);

            var operations = topDownIndex.getContainedOperations(service).stream().sorted().toList();
            GeneratorUtils.generateOperationListing(context, writer, service, operations);

            writeAuthSection(context, writer, service);

            writer.closeHeading();
            writer.popState();
        });
    }

    private void writeAuthSection(DocGenerationContext context, DocWriter writer, ServiceShape service) {
        var authSchemes = DocgenUtils.getPrioritizedServiceAuth(context.model(), service);
        if (authSchemes.isEmpty()) {
            return;
        }

        writer.pushState(new AuthSection(context, service));
        writer.openHeading("Auth");

        var index = ServiceIndex.of(context.model());
        writer.putContext("optional",
                index.getEffectiveAuthSchemes(service, AuthSchemeMode.NO_AUTH_AWARE)
                        .containsKey(NoAuthTrait.ID));
        writer.putContext("multipleSchemes", authSchemes.size() > 1);
        writer.write("""
                Operations on the service ${?optional}may optionally${/optional}${^optional}MUST${/optional} \
                be called with ${?multipleSchemes}one of the following priority-ordered auth schemes${/multipleSchemes}\
                ${^multipleSchemes}the following auth scheme${/multipleSchemes}. Additionally, authentication for \
                individual operations may be optional${?multipleSchemes}, have a different priority order, support \
                fewer schemes,${/multipleSchemes} or be disabled entirely.
                """);

        writer.openDefinitionList();

        for (var scheme : authSchemes) {
            var authTraitShape = context.model().expectShape(scheme);
            var authTraitSymbol = context.symbolProvider().toSymbol(authTraitShape);

            writer.pushState(new ShapeSection(context, authTraitShape));
            writer.openDefinitionListItem(w -> w.write("$R", authTraitSymbol));

            writer.injectSection(new ShapeSubheadingSection(context, authTraitShape));
            writer.writeShapeDocs(authTraitShape, context.model());
            writer.injectSection(new ShapeDetailsSection(context, authTraitShape));

            writer.closeDefinitionListItem();
            writer.popState();
        }

        writer.closeDefinitionList();
        writer.closeHeading();
        writer.popState();
    }
}

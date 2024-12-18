/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.traits.ReferencesTrait.Reference;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds a "see also" to structures / operations that reference resources using
 * the <a href="https://smithy.io/2.0/spec/resource-traits.html#references-trait">references trait</a>.
 */
@SmithyInternalApi
public final class ReferencesInterceptor implements CodeInterceptor.Appender<ShapeDetailsSection, DocWriter> {
    private static final Logger LOGGER = Logger.getLogger(ReferencesInterceptor.class.getName());

    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeDetailsSection section) {
        var model = section.context().model();
        if (model.getResourceShapes().isEmpty() && section.context().settings().references().isEmpty()) {
            // If there's nothing referenceable, we can return quickly.
            return false;
        }

        if (section.shape().isMemberShape()) {
            // Since the containing shape will show information about the reference, it's not
            // necessary to also show that on the members.
            return false;
        }

        return !getLocalReferences(section.context(), section.shape()).isEmpty();
    }

    @Override
    public void append(DocWriter writer, ShapeDetailsSection section) {
        var model = section.context().model();
        var symbolProvider = section.context().symbolProvider();
        var localRefs = getLocalReferences(section.context(), section.shape());
        var externalRefs = section.context().settings().references();
        var serviceResources = TopDownIndex.of(model)
                .getContainedResources(section.context().settings().service())
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());

        // This is a mapping of reference link to optional rel type. If `rel` isn't set,
        // it'll be an empty optional that won't get displayed.
        var references = new LinkedHashMap<>(localRefs.size());
        for (var reference : localRefs) {
            if (serviceResources.contains(reference.getResource())) {
                var symbol = symbolProvider.toSymbol(model.expectShape(reference.getResource()));
                references.put(symbol, reference.getRel());
            } else if (externalRefs.containsKey(reference.getResource())) {
                var ref = Pair.of(reference.getResource().getName(), externalRefs.get(reference.getResource()));
                references.put(ref, reference.getRel());
            }
        }

        writer.pushState();
        writer.putContext("refs", references);
        writer.putContext("multipleRefs", references.size() > 1);
        writer.openAdmonition(NoticeType.INFO);
        writer.write("""
                This references \
                ${?multipleRefs}the following resources: ${/multipleRefs}\
                ${^multipleRefs}the resource ${/multipleRefs}\
                ${#refs}
                ${key:R}${?value} (rel type: ${value:`})${/value}${^key.last}, ${/key.last}\
                ${/refs}
                .
                """);
        writer.closeAdmonition();
        writer.popState();
    }

    private Set<Reference> getLocalReferences(DocGenerationContext context, Shape shape) {
        var model = context.model();
        var references = new LinkedHashSet<Reference>();
        if (shape.isOperationShape()) {
            var operation = shape.asOperationShape().get();
            references.addAll(getLocalReferences(context, model.expectShape(operation.getInputShape())));
            references.addAll(getLocalReferences(context, model.expectShape(operation.getInputShape())));
            return references;
        }
        for (var member : shape.members()) {
            references.addAll(getLocalReferences(context, member));
        }

        var shapeRefs = shape.getMemberTrait(model, ReferencesTrait.class);
        var externalsRefs = context.settings().references();
        var serviceResources = TopDownIndex.of(model)
                .getContainedResources(context.settings().service())
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());

        if (shapeRefs.isPresent()) {
            for (var reference : shapeRefs.get().getReferences()) {
                if (serviceResources.contains(reference.getResource())
                        || externalsRefs.containsKey(reference.getResource())) {
                    references.add(reference);
                } else {
                    LOGGER.warning(String.format("""
                            Unable to generate a reference link for `%s`, referenced by `%s`. Use the `references` \
                            map in the generator settings to add a reference link.""",
                            reference.getResource(),
                            shape.getId()));
                }
            }
        }
        return references;
    }
}

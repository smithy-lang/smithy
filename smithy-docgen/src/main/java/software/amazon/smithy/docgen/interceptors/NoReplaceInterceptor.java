/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import java.util.Optional;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.knowledge.BottomUpIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.NoReplaceTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds a note that a resource's put operation can't do updates if it has the
 * <a href="https://smithy.io/2.0/spec/resource-traits.html#noreplace-trait">
 * noReplace trait</a>.
 */
@SmithyInternalApi
abstract class NoReplaceInterceptor<S extends CodeSection> implements CodeInterceptor<S, DocWriter> {
    @Override
    public boolean isIntercepted(S section) {
        var shape = getShape(section);
        var resource = getResource(getContext(section), shape);
        return resource.isPresent()
                && resource.get().hasTrait(NoReplaceTrait.class)
                && resource.get().getPut().map(put -> put.equals(shape.getId())).orElse(false);
    }

    @Override
    public void write(
            DocWriter writer,
            String previousText,
            S section
    ) {
        var context = getContext(section);
        var resource = getResource(context, getShape(section)).get();
        var resourceReference = SymbolReference.builder()
                .alias("resource")
                .symbol(context.symbolProvider().toSymbol(resource))
                .build();
        var updateSymbolReference = resource.getUpdate()
                .map(update -> context.model().expectShape(update))
                .map(update -> context.symbolProvider().toSymbol(update))
                .map(symbol -> SymbolReference.builder().alias("update lifecycle operation").symbol(symbol).build());
        writer.putContext("update", updateSymbolReference);
        writer.writeWithNoFormatting(previousText);
        writer.openAdmonition(NoticeType.NOTE);
        writer.write("""
                This operation cannot be used to update the $1R.\
                ${?update} To update the $1R, use the ${update:R}.${/update}""",
                resourceReference);
        writer.closeAdmonition();
    }

    /**
     * Extracts the shape for the section.
     * @param section the section to extract the shape from.
     * @return returns the section's shape.
     */
    abstract Shape getShape(S section);

    /**
     * Extracts the context for the section.
     * @param section the section to extract the context from.
     * @return returns the section's context.
     */
    abstract DocGenerationContext getContext(S section);

    private Optional<ResourceShape> getResource(DocGenerationContext context, Shape shape) {
        var bottomUpIndex = BottomUpIndex.of(context.model());
        return bottomUpIndex.getResourceBinding(context.settings().service(), shape);
    }

}

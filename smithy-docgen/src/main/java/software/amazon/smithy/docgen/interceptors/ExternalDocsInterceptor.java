/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds external doc links after a shape's modeled docs based on the
 * <a href="https://smithy.io/2.0/spec/documentation-traits.html#externaldocumentation-trait">
 * externalDocumentation</a> trait.
 */
@SmithyInternalApi
public final class ExternalDocsInterceptor implements CodeInterceptor<ShapeDetailsSection, DocWriter> {
    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeDetailsSection section) {
        return section.shape().getMemberTrait(section.context().model(), ExternalDocumentationTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeDetailsSection section) {
        var trait = section.shape().getMemberTrait(section.context().model(), ExternalDocumentationTrait.class).get();
        writer.openAdmonition(NoticeType.INFO);
        trait.getUrls()
                .entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .forEach(pair -> writer.write("$R\n", pair));
        writer.closeAdmonition();
        writer.writeWithNoFormatting(previousText);
    }
}

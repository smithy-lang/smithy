/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import java.util.Locale;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds an admonition to shapes that have the
 * <a href="https://smithy.io/2.0/spec/type-refinement-traits.html#sparse-trait">
 * sparse trait</a>.
 */
@SmithyInternalApi
public final class SparseInterceptor implements CodeInterceptor<ShapeDetailsSection, DocWriter> {
    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeDetailsSection section) {
        return section.shape().getMemberTrait(section.context().model(), SparseTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeDetailsSection section) {
        var target = section.shape().isMemberShape()
                ? section.context().model().expectShape(section.shape().asMemberShape().get().getTarget())
                : section.shape();
        writer.writeWithNoFormatting(previousText);
        writer.openAdmonition(NoticeType.NOTE);
        writer.write("This $L may contain null values.", target.getType().toString().toLowerCase(Locale.ENGLISH));
        writer.closeAdmonition();
    }
}

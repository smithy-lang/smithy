/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.RecommendedTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds a notice for recommended members based on the
 * <a href="https://smithy.io/2.0/spec/documentation-traits.html#recommended-trait">
 * recommended trait</a>.
 */
@SmithyInternalApi
public final class RecommendedInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().hasTrait(RecommendedTrait.class);
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        var trait = section.shape().expectTrait(RecommendedTrait.class);
        writer.putContext("reason", trait.getReason());
        writer.writeBadge(NoticeType.IMPORTANT, "RECOMMENDED");
        writer.write("""
                ${?reason} ${reason:L}${/reason}

                $L""", previousText);
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds deprecation warnings to shape docs.
 */
@SmithyInternalApi
public final class DeprecatedInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().getMemberTrait(section.context().model(), DeprecatedTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        var trait = section.shape().getMemberTrait(section.context().model(), DeprecatedTrait.class).get();
        writer.putContext("since", trait.getSince());
        writer.openAdmonition(NoticeType.WARNING, w -> {
            w.write("Deprecated${?since} since ${since:L}${/since}");
        });
        writer.putContext("message", trait.getMessage());
        writer.write("""
                ${?message}${message:L}${/message}
                ${^message}This has been deprecated${?since} since version ${since:L}${/since}.${/message}
                """);
        writer.closeAdmonition();
        writer.writeWithNoFormatting(previousText);
    }
}

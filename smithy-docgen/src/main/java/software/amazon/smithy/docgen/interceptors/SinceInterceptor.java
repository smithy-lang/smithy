/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.SinceTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds admonitions for when a shape was introduced based on the
 * <a href="https://smithy.io/2.0/spec/documentation-traits.html#smithy-api-since-trait">
 * since trait</a>.
 */
@SmithyInternalApi
public final class SinceInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().getMemberTrait(section.context().model(), SinceTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        var trait = section.shape().getMemberTrait(section.context().model(), SinceTrait.class).get();
        writer.openAdmonition(NoticeType.NOTE);
        writer.write("New in version $L.", trait.getValue());
        writer.closeAdmonition();
        writer.writeWithNoFormatting(previousText);
    }
}

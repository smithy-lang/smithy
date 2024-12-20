/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds danger admonitions to shapes that have the
 * <a href="https://smithy.io/2.0/spec/documentation-traits.html#sensitive-trait">
 * sensitive trait</a>.
 */
@SmithyInternalApi
public final class SensitiveInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().getMemberTrait(section.context().model(), SensitiveTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        writer.openAdmonition(NoticeType.DANGER);
        writer.write("""
                The data this contains is sensitive and MUST be handled with care. \
                It MUST NOT be exposed in things like exception messages or log \
                output, except for full wire logs.""");
        writer.closeAdmonition();
        writer.writeWithNoFormatting(previousText);
    }
}

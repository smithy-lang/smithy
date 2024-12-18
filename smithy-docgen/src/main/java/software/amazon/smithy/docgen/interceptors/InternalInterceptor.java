/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import java.util.logging.Logger;
import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.InternalTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds an admonition to shapes marked as
 * <a href="https://smithy.io/2.0/spec/documentation-traits.html#internal-trait">
 * internal</a>.
 */
@SmithyInternalApi
public final class InternalInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    private static final Logger LOGGER = Logger.getLogger(InternalInterceptor.class.getName());

    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().getMemberTrait(section.context().model(), InternalTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        // TODO: add a DANGER-level validator
        LOGGER.warning(String.format("""
                Internal shape %s found. Adding DANGER admonition to its documentation. \
                If this isn't meant to be documented, use a trait filter in your projection \
                to filter out internal shapes: \
                https://smithy.io/2.0/guides/building-models/build-config.html#excludeshapesbytrait""",
                section.shape().getId()));
        writer.openAdmonition(NoticeType.DANGER);
        writer.write("This is part of the internal API not available to external customers.");
        writer.closeAdmonition();
        writer.writeWithNoFormatting(previousText);
    }
}

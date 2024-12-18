/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds documentation for the <a href="https://smithy.io/2.0/spec/streaming.html">streaming trait</a>.
 */
@SmithyInternalApi
public class StreamingInterceptor implements CodeInterceptor.Appender<ShapeDetailsSection, DocWriter> {
    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeDetailsSection section) {
        return section.shape().getMemberTrait(section.context().model(), StreamingTrait.class).isPresent();
    }

    @Override
    public void append(DocWriter writer, ShapeDetailsSection section) {
        var target = section.shape()
                .asMemberShape()
                .map(member -> section.context().model().expectShape(member.getTarget()))
                .orElse(section.shape());
        if (target.isBlobShape()) {
            writer.pushState();
            writer.putContext("requiresLength", target.hasTrait(RequiresLengthTrait.class));
            writer.openAdmonition(NoticeType.IMPORTANT);
            writer.write("""
                    The data in this member is potentially very large and therefore must be streamed and not \
                    stored in memory.${?requiresLength} The size of the data must be known ahead of time.\
                    ${/requiresLength}
                    """);
            writer.closeAdmonition();
            writer.popState();
            return;
        }

        // TODO: event streams
    }
}

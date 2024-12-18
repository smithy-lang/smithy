/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.knowledge.NullableIndex.CheckMode;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds nullability information to member docs.
 */
@SmithyInternalApi
public final class NullabilityInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        if (!section.shape().isMemberShape()) {
            return false;
        }

        // It might seem crazy to create this for *every member*, but knowledge indexes
        // actually get cached on the model so in reality it's only created once.
        var index = NullableIndex.of(section.context().model());
        return !index.isMemberNullable(section.shape().asMemberShape().get(), CheckMode.SERVER);
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        writer.writeBadge(NoticeType.WARNING, "REQUIRED")
                .write("\n\n$L", previousText);
    }
}

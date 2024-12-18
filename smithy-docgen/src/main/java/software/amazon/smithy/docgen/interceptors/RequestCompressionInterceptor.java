/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import java.util.Optional;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information about request compression to operations with the
 * <a href="https://smithy.io/2.0/spec/behavior-traits.html#requestcompression-trait">
 * requestCompression trait</a>.
 */
@SmithyInternalApi
public final class RequestCompressionInterceptor implements CodeInterceptor<ShapeDetailsSection, DocWriter> {
    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeDetailsSection section) {
        return section.shape().hasTrait(RequestCompressionTrait.class);
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeDetailsSection section) {
        var trait = section.shape().expectTrait(RequestCompressionTrait.class);
        writer.openAdmonition(NoticeType.IMPORTANT);

        // Have particular support for single-element lists.
        writer.putContext("encoding",
                trait.getEncodings().size() == 1
                        ? Optional.of(trait.getEncodings().get(0))
                        : Optional.empty());
        writer.putContext("encodings", trait.getEncodings());

        writer.write("""
                This operation supports optional request compression using \
                ${?encoding}${encoding:L} encoding.${/encoding}\
                ${^encoding}one of the following priority-ordered encodings: \
                ${#encodings}${value:L}${^key.last}, ${/key.last}${/encodings}.${/encoding}""");
        writer.closeAdmonition();
        writer.writeWithNoFormatting(previousText);
    }
}

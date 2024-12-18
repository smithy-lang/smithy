/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

// TODO: Add content type to operation docs. Need a way to determine http protocols first.
/**
 * Adds the media type to member documentation if it has the
 * <a href="https://smithy.io/2.0/spec/protocol-traits.html#smithy-api-mediatype-trait">
 * mediaType</a> trait.
 */
@SmithyInternalApi
public final class MediaTypeInterceptor implements CodeInterceptor.Prepender<ShapeSubheadingSection, DocWriter> {
    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().getMemberTrait(section.context().model(), MediaTypeTrait.class).isPresent();
    }

    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public void prepend(DocWriter writer, ShapeSubheadingSection section) {
        var trait = section.shape().getMemberTrait(section.context().model(), MediaTypeTrait.class).get();
        writer.write("""
                $B $`

                """, "Media Type:", trait.getValue());
    }
}

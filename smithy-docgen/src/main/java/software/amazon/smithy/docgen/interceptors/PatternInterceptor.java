/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information to shapes if they have the
 * <a href="https://smithy.io/2.0/spec/constraint-traits.html#pattern-trait">
 * pattern trait</a>.
 */
@SmithyInternalApi
public final class PatternInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    private static final Pair<String, String> REGEX_REF = Pair.of(
            "ECMA 262 regular expression",
            "https://262.ecma-international.org/8.0/#sec-patterns");

    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().getMemberTrait(section.context().model(), PatternTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        var trait = section.shape().getMemberTrait(section.context().model(), PatternTrait.class).get();
        writer.write("""
                This value must match the following $R: $`

                $L""", REGEX_REF, trait.getValue(), previousText);
    }
}

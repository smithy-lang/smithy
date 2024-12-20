/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information to shapes if they have the
 * <a href="https://smithy.io/2.0/spec/constraint-traits.html#range-trait">
 * range trait</a>.
 */
@SmithyInternalApi
public final class RangeInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().getMemberTrait(section.context().model(), RangeTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        var trait = section.shape().getMemberTrait(section.context().model(), RangeTrait.class).get();
        writer.putContext("min", trait.getMin());
        writer.putContext("max", trait.getMax());

        writer.write("""
                ${?min}
                $1B ${min:L}

                ${/min}
                ${?max}
                $2B ${max:L}

                ${/max}
                $3L""",
                "Minimum:",
                "Maximum:",
                previousText);
    }
}

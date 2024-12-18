/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information to shapes if they have the
 * <a href="https://smithy.io/2.0/spec/constraint-traits.html#length-trait">
 * length trait</a>.
 */
@SmithyInternalApi
public final class LengthInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    private static final Pair<String, String> UNICODE_SCALAR_VALUE_REFERENCE = Pair.of(
            "Unicode scalar values",
            "https://www.unicode.org/glossary/#unicode_scalar_value");

    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().getMemberTrait(section.context().model(), LengthTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        var trait = section.shape().getMemberTrait(section.context().model(), LengthTrait.class).get();
        writer.putContext("min", trait.getMin());
        writer.putContext("max", trait.getMax());

        var target = section.shape().isMemberShape()
                ? section.context().model().expectShape(section.shape().asMemberShape().get().getTarget())
                : section.shape();

        writer.write("""
                ${?min}
                $1B ${min:L} $3C.

                ${/min}
                ${?max}
                $2B ${max:L} $3C.

                ${/max}
                $4L""",
                "Minimum length:",
                "Maximum length:",
                writer.consumer(w -> writeUnit(w, target)),
                previousText);
    }

    private void writeUnit(DocWriter writer, Shape target) {
        switch (target.getType()) {
            case MAP -> writer.writeInline("pairs");
            case STRING -> writer.writeInline("$R", UNICODE_SCALAR_VALUE_REFERENCE);
            case BLOB -> writer.writeInline("bytes");
            default -> writer.writeInline("elements");
        }
    }
}

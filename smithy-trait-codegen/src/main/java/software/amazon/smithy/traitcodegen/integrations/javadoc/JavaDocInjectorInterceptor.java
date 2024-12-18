/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.sections.EnumVariantSection;
import software.amazon.smithy.traitcodegen.sections.GetterSection;
import software.amazon.smithy.traitcodegen.sections.JavaDocSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;

/**
 * Adds a javadoc section to classes, getters, and enum variants.
 *
 * <p>The injected javadoc section does not contain any javadoc contents when it is first injected. This
 * section is instead populated by other {@code CodeInterceptors} that trigger off of specific traits.
 * Note: This interceptor will also add any relevant documentation annotations to classes, getters, or enum variants.
 */
final class JavaDocInjectorInterceptor implements CodeInterceptor.Prepender<CodeSection, TraitCodegenWriter> {
    @Override
    public Class<CodeSection> sectionType() {
        return CodeSection.class;
    }

    @Override
    public boolean isIntercepted(CodeSection section) {
        return section instanceof ClassSection
                || section instanceof GetterSection
                || section instanceof EnumVariantSection;
    }

    @Override
    public void prepend(TraitCodegenWriter writer, CodeSection section) {
        Shape shape;
        if (section instanceof ClassSection) {
            shape = ((ClassSection) section).shape();
        } else if (section instanceof GetterSection) {
            shape = ((GetterSection) section).shape();
        } else if (section instanceof EnumVariantSection) {
            shape = ((EnumVariantSection) section).memberShape();
        } else {
            throw new IllegalArgumentException("Javadocs cannot be injected for section: " + section);
        }

        writer.injectSection(new JavaDocSection(shape));
    }
}

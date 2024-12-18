/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.traitcodegen.sections.JavaDocSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Adds Javadoc documentation for the {@link DocumentationTrait}.
 *
 * <p>The documentation trait contents are added as the contents of the Javadoc section.
 */
final class DocumentationTraitInterceptor implements CodeInterceptor<JavaDocSection, TraitCodegenWriter> {

    @Override
    public void write(TraitCodegenWriter writer, String previousText, JavaDocSection section) {
        writer.write(section.shape().expectTrait(DocumentationTrait.class).getValue());

        if (!previousText.isEmpty()) {
            // Add spacing if tags have been added to the javadoc
            writer.newLine();
            writer.writeInlineWithNoFormatting(previousText);
        }
    }

    @Override
    public Class<JavaDocSection> sectionType() {
        return JavaDocSection.class;
    }

    @Override
    public boolean isIntercepted(JavaDocSection section) {
        return section.shape().hasTrait(DocumentationTrait.class);
    }
}

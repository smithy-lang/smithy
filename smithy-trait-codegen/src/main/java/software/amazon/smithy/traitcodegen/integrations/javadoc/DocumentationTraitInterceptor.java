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
        String docs = section.shape().expectTrait(DocumentationTrait.class).getValue();
        writer.write(escapeAt(docs));

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
        return section.shape().hasTrait(DocumentationTrait.ID);
    }

    // Escapes '@' at the beginning of a line to prevent javac from interpreting
    // it as a Javadoc block tag.
    private static String escapeAt(String docs) {
        StringBuilder result = new StringBuilder(docs.length());
        boolean lineStart = true;
        for (int i = 0; i < docs.length(); i++) {
            char current = docs.charAt(i);
            if (current == '@' && lineStart) {
                if (i + 1 < docs.length() && docs.charAt(i + 1) == '@') {
                    result.append("@@");
                    i++;
                } else {
                    result.append("{@literal @}");
                }
            } else {
                result.append(current);
            }
            lineStart = current == '\n';
        }
        return result.toString();
    }
}

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
        // We don't pass doc comment directly, as a first argument, and instead do it using L formatter.
        // We do that for two reasons:
        // 1. avoid interpreting expression start character (dollar sign ($) by default) as special character
        // 2. let L formatter, overridden in TraitCodegenWriter, to escape this character for us under the hood.
        // It is needed because TraitCodegenWriter performs rendering in two stages:
        // 1. first it renders everything except references to types (see software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter.JavaTypeFormatter.getPlaceholder)
        // 2. then, in toString method, it renders the rest.
        // Because of that, if an (already rendered) string literal (e.g. a doc comment after this line of code)
        // contains an unescaped expression start symbol ($) it'd be parsed incorrectly on the second rendering and lead to an error.
        writer.write("$L", escapeAt(docs));

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

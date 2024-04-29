/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.javadoc;

import java.util.Map;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.traitcodegen.sections.JavaDocSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Adds the javadoc {@code @see} tag to the generated javadocs if the corresponding smithy shape
 * has the {@link ExternalDocumentationTrait} trait applied.
 */
final class ExternalDocsInterceptor implements CodeInterceptor.Appender<JavaDocSection, TraitCodegenWriter> {

    @Override
    public void append(TraitCodegenWriter writer, JavaDocSection section) {
        ExternalDocumentationTrait trait = section.shape().expectTrait(ExternalDocumentationTrait.class);
        // Add a space to make it easier to read
        writer.writeDocStringContents("");
        for (Map.Entry<String, String> entry : trait.getUrls().entrySet()) {
            writer.writeDocStringContents("@see <a href=$S>$L</a>", entry.getValue(), entry.getKey());
        }
    }

    @Override
    public Class<JavaDocSection> sectionType() {
        return JavaDocSection.class;
    }

    @Override
    public boolean isIntercepted(JavaDocSection section) {
        return section.shape().hasTrait(ExternalDocumentationTrait.class);
    }
}

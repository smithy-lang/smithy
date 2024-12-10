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
 *
 * <p>Note: This interceptor should run before the {@link DeprecatedInterceptor} and {@link SinceInterceptor}
 * to ensure proper ordering of Javadoc tags.
 */
final class ExternalDocumentationInterceptor implements CodeInterceptor.Appender<JavaDocSection, TraitCodegenWriter> {

    @Override
    public void append(TraitCodegenWriter writer, JavaDocSection section) {
        ExternalDocumentationTrait trait = section.shape().expectTrait(ExternalDocumentationTrait.class);
        for (Map.Entry<String, String> entry : trait.getUrls().entrySet()) {
            writer.write("@see <a href=$S>$L</a>", entry.getValue(), entry.getKey());
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

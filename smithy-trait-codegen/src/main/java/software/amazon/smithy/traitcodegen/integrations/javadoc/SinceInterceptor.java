/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.model.traits.SinceTrait;
import software.amazon.smithy.traitcodegen.sections.JavaDocSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Adds the {@code @since} javadoc tag to generated Javadoc documentation for a class
 * if the smithy shape the class corresponds to has the {@link SinceTrait} trait applied.
 *
 * <p>Note: This interceptor should be run before {@link DeprecatedInterceptor} and after
 * {@link ExternalDocumentationInterceptor} to ensure proper tag ordering.
 */
final class SinceInterceptor implements CodeInterceptor.Appender<JavaDocSection, TraitCodegenWriter> {
    @Override
    public void append(TraitCodegenWriter writer, JavaDocSection section) {
        SinceTrait trait = section.shape().expectTrait(SinceTrait.class);
        writer.write("@since $L", trait.getValue());
    }

    @Override
    public Class<JavaDocSection> sectionType() {
        return JavaDocSection.class;
    }

    @Override
    public boolean isIntercepted(JavaDocSection section) {
        return section.shape().hasTrait(SinceTrait.class);
    }
}

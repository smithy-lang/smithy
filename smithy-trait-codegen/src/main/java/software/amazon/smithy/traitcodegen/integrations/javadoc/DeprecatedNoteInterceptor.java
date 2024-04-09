/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.traitcodegen.sections.JavaDocSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Adds the {@code @deprecated} javadoc tag to generated Javadoc documentation for a class
 * if the smithy shape the class corresponds to has the {@link DeprecatedTrait} trait applied.
 *
 * <p>If the {@code DeprecatedTrait} contains a {@code since} field, then "As of " note will be added
 * to the generated tag.
 */
final class DeprecatedNoteInterceptor implements CodeInterceptor.Appender<JavaDocSection, TraitCodegenWriter> {
    @Override
    public void append(TraitCodegenWriter writer, JavaDocSection section) {
        DeprecatedTrait trait = section.shape().expectTrait(DeprecatedTrait.class);
        writer.putContext("since", trait.getSince());
        // Add spacing
        writer.writeDocStringContents("");
        writer.writeDocStringContents("@deprecated ${?since}As of ${since:L}. ${/since}$L", trait.getMessage());
    }

    @Override
    public Class<JavaDocSection> sectionType() {
        return JavaDocSection.class;
    }

    @Override
    public boolean isIntercepted(JavaDocSection section) {
        return section.shape().hasTrait(DeprecatedTrait.class);
    }
}

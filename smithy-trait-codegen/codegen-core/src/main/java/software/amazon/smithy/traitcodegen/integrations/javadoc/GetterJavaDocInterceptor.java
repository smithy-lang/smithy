/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.traitcodegen.sections.GetterSection;
import software.amazon.smithy.traitcodegen.sections.JavaDocSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Adds Javadocs to the getter for a member if the corresponding member has a {@link DocumentationTrait}
 * trait applied. The Javadoc contents will just be the contents of the {@code DocumentationTrait}.
 */
final class GetterJavaDocInterceptor implements CodeInterceptor.Prepender<GetterSection, TraitCodegenWriter> {
    @Override
    public void prepend(TraitCodegenWriter writer, GetterSection section) {
        DocumentationTrait trait = section.shape().expectTrait(DocumentationTrait.class);
        writer.newLine();
        writer.openDocstring();
        writer.pushState(new JavaDocSection(section.shape()));
        writer.writeDocStringContents(trait.getValue());
        writer.popState();
        writer.closeDocstring();
    }

    @Override
    public Class<GetterSection> sectionType() {
        return GetterSection.class;
    }

    @Override
    public boolean isIntercepted(GetterSection section) {
        return section.shape().hasTrait(DocumentationTrait.class)
                && section.shape().isMemberShape();
    }
}

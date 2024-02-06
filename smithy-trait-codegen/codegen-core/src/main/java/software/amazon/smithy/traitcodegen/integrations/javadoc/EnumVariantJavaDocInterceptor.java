/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.traitcodegen.sections.EnumVariantSection;
import software.amazon.smithy.traitcodegen.sections.JavaDocSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Adds a docstring to each java enum variant if the corresponding enum member has a doc comment.
 */
final class EnumVariantJavaDocInterceptor implements CodeInterceptor.Prepender<EnumVariantSection,
        TraitCodegenWriter> {
    @Override
    public void prepend(TraitCodegenWriter writer, EnumVariantSection section) {
        DocumentationTrait trait = section.memberShape().expectTrait(DocumentationTrait.class);
        writer.newLine();
        writer.openDocstring();
        writer.pushState(new JavaDocSection(section.memberShape()));
        writer.writeDocStringContents(trait.getValue());
        writer.popState();
        writer.closeDocstring();
    }

    @Override
    public Class<EnumVariantSection> sectionType() {
        return EnumVariantSection.class;
    }

    @Override
    public boolean isIntercepted(EnumVariantSection section) {
        return section.memberShape().hasTrait(DocumentationTrait.class);
    }
}

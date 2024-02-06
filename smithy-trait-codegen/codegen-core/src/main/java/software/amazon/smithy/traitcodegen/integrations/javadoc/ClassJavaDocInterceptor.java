/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.sections.JavaDocSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Adds basic JavaDocs for generated java classes..
 */
final class ClassJavaDocInterceptor implements CodeInterceptor.Prepender<ClassSection, TraitCodegenWriter> {
    @Override
    public void prepend(TraitCodegenWriter writer, ClassSection section) {
        writer.openDocstring();
        writer.pushState(new JavaDocSection(section.shape()));
        writer.writeDocStringContents(section.shape().expectTrait(DocumentationTrait.class).getValue());
        writer.popState();
        writer.closeDocstring();
    }

    @Override
    public Class<ClassSection> sectionType() {
        return ClassSection.class;
    }

    @Override
    public boolean isIntercepted(ClassSection section) {
        return section.shape().hasTrait(DocumentationTrait.class);
    }
}

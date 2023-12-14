/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.traitcodegen.sections.ToBuilderSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

final class BuilderMethodDocsInterceptor implements CodeInterceptor.Prepender<ToBuilderSection, TraitCodegenWriter> {
    @Override
    public void prepend(TraitCodegenWriter writer, ToBuilderSection section) {
        writer.openDocstring();
        writer.writeDocStringContents("Creates a builder used to build a {@link $T}.", section.symbol());
        writer.closeDocstring();
    }

    @Override
    public Class<ToBuilderSection> sectionType() {
        return ToBuilderSection.class;
    }
}

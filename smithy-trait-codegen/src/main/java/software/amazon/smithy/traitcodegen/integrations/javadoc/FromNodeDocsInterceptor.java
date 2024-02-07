/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.traitcodegen.sections.FromNodeSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Adds a docstring for the {@code fromNode} method.
 */
final class FromNodeDocsInterceptor
        implements CodeInterceptor.Prepender<FromNodeSection, TraitCodegenWriter> {
    @Override
    public void prepend(TraitCodegenWriter writer, FromNodeSection section) {
        writer.openDocstring();
        writer.writeDocStringContents("Creates a {@link $T} from a {@link Node}.", section.symbol());
        writer.writeDocStringContents("");
        writer.writeDocStringContents("@param node Node to create the $T from.", section.symbol());
        writer.writeDocStringContents("@return Returns the created $T.", section.symbol());
        writer.writeDocStringContents("@throws software.amazon.smithy.model.node.ExpectationNotMetException "
                + "if the given Node is invalid.");
        writer.closeDocstring();
    }

    @Override
    public Class<FromNodeSection> sectionType() {
        return FromNodeSection.class;
    }
}

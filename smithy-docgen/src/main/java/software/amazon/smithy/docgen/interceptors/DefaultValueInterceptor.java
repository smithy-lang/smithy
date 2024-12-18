/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds default value information to trait documentation.
 */
@SmithyInternalApi
public final class DefaultValueInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().getMemberTrait(section.context().model(), DefaultTrait.class).isPresent();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        var defaultValue = section.shape().getMemberTrait(section.context().model(), DefaultTrait.class).get().toNode();
        writer.write("""
                $B $`

                $L""", "Default Value:", Node.printJson(defaultValue), previousText);
    }
}

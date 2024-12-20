/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds a line to the error shape docs to indicate whether the error is a client or
 * service error.
 */
@SmithyInternalApi
public final class ErrorFaultInterceptor implements CodeInterceptor<ShapeSubheadingSection, DocWriter> {
    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().hasTrait(ErrorTrait.class);
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeSubheadingSection section) {
        var fault = section.shape().expectTrait(ErrorTrait.class).getValue();
        writer.write("""
                This is an error caused by the $L.

                $L
                """, fault, previousText);
    }
}

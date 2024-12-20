/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds the noReplace admonition to the resource operation's doc page.
 */
@SmithyInternalApi
public final class NoReplaceOperationInterceptor extends NoReplaceInterceptor<ShapeDetailsSection> {
    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    Shape getShape(ShapeDetailsSection section) {
        return section.shape();
    }

    @Override
    DocGenerationContext getContext(ShapeDetailsSection section) {
        return section.context();
    }
}

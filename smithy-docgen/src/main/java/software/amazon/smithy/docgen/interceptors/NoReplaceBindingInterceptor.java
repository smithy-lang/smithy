/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.sections.LifecycleOperationSection;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds the noReplace admonition to the resource lifecycle property list on the resource page.
 */
@SmithyInternalApi
public final class NoReplaceBindingInterceptor extends NoReplaceInterceptor<LifecycleOperationSection> {
    @Override
    public Class<LifecycleOperationSection> sectionType() {
        return LifecycleOperationSection.class;
    }

    @Override
    Shape getShape(LifecycleOperationSection section) {
        return section.operation();
    }

    @Override
    DocGenerationContext getContext(LifecycleOperationSection section) {
        return section.context();
    }
}

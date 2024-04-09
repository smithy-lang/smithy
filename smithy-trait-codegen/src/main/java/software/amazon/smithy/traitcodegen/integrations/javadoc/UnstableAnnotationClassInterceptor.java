/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.javadoc;

import software.amazon.smithy.model.traits.UnstableTrait;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Adds the {@code @SmithyUnstableApi} annotation to generated classes if the smithy shape the class corresponds to
 * has the {@link UnstableTrait} trait applied.
 */
final class UnstableAnnotationClassInterceptor implements CodeInterceptor.Prepender<ClassSection, TraitCodegenWriter> {

    @Override
    public void prepend(TraitCodegenWriter writer, ClassSection section) {
        writer.write("@$T", SmithyUnstableApi.class);
    }

    @Override
    public Class<ClassSection> sectionType() {
        return ClassSection.class;
    }

    @Override
    public boolean isIntercepted(ClassSection section) {
        return section.shape().hasTrait(UnstableTrait.class);
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.annotations;

import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Adds the {@link software.amazon.smithy.utils.SmithyGenerated} annotation to all generated classes.
 */
final class SmithyGeneratedAnnotationInterceptor
        implements CodeInterceptor.Prepender<ClassSection, TraitCodegenWriter> {
    @Override
    public Class<ClassSection> sectionType() {
        return ClassSection.class;
    }

    @Override
    public void prepend(TraitCodegenWriter writer, ClassSection section) {
        writer.write("@$T", SmithyGenerated.class);
    }
}

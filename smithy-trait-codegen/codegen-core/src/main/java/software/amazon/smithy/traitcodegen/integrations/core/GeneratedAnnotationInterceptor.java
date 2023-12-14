/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.core;

import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyGenerated;

final class GeneratedAnnotationInterceptor implements CodeInterceptor.Prepender<ClassSection,
        TraitCodegenWriter> {
    @Override
    public void prepend(TraitCodegenWriter writer, ClassSection section) {
        writer.addImport(SmithyGenerated.class);
        writer.write("@SmithyGenerated");
    }

    @Override
    public Class<ClassSection> sectionType() {
        return ClassSection.class;
    }
}

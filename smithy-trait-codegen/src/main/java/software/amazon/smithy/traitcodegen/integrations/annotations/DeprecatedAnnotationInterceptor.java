/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.annotations;

import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.sections.EnumVariantSection;
import software.amazon.smithy.traitcodegen.sections.GetterSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;

/**
 * Adds the {@link Deprecated} annotation to generated code for shapes with the {@link DeprecatedTrait}.
 */
final class DeprecatedAnnotationInterceptor implements CodeInterceptor.Prepender<CodeSection, TraitCodegenWriter> {

    @Override
    public Class<CodeSection> sectionType() {
        return CodeSection.class;
    }

    @Override
    public boolean isIntercepted(CodeSection section) {
        if (section instanceof ClassSection) {
            return ((ClassSection) section).shape().hasTrait(DeprecatedTrait.class);
        } else if (section instanceof GetterSection) {
            return ((GetterSection) section).shape().hasTrait(DeprecatedTrait.class);
        } else if (section instanceof EnumVariantSection) {
            return ((EnumVariantSection) section).memberShape().hasTrait(DeprecatedTrait.class);
        }
        return false;
    }

    @Override
    public void prepend(TraitCodegenWriter writer, CodeSection section) {
        writer.write("@$T", Deprecated.class);
    }
}

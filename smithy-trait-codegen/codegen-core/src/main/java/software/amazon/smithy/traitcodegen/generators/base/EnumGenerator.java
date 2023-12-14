/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.base;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.directed.GenerateEnumDirective;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;

public final class EnumGenerator extends AbstractEnumGenerator<GenerateEnumDirective<TraitCodegenContext,
        TraitCodegenSettings>> {
    private static final String VARIANT_TEMPLATE = "$L($S)";
    private static final Symbol VALUE_TYPE = TraitCodegenUtils.fromClass(String.class);

    @Override
    public void accept(GenerateEnumDirective<TraitCodegenContext, TraitCodegenSettings> directive) {
        directive.context().writerDelegator().useShapeWriter(directive.shape(),
                writer -> writeEnum(directive.shape(), directive.symbolProvider(), writer, directive.model()));
    }

    @Override
    String getVariantTemplate() {
        return VARIANT_TEMPLATE;
    }

    @Override
    Symbol getValueType() {
        return VALUE_TYPE;
    }

    @Override
    Object getEnumValue(MemberShape member) {
        return member.expectTrait(EnumValueTrait.class).expectStringValue();
    }
}

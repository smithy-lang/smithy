/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.base;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.directed.GenerateIntEnumDirective;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.utils.SymbolUtil;

public class IntEnumGenerator extends AbstractEnumGenerator<GenerateIntEnumDirective<TraitCodegenContext,
        TraitCodegenSettings>> {
    private static final String VARIANT_TEMPLATE = "$L($L)";
    private static final Symbol VALUE_TYPE = SymbolUtil.fromClass(int.class);

    @Override
    public void accept(GenerateIntEnumDirective<TraitCodegenContext, TraitCodegenSettings> directive) {
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
        return member.expectTrait(EnumValueTrait.class).expectIntValue();
    }
}

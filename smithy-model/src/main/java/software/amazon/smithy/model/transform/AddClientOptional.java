/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

final class AddClientOptional {

    private final boolean applyWhenNoDefaultValue;

    AddClientOptional(boolean applyWhenNoDefaultValue) {
        this.applyWhenNoDefaultValue = applyWhenNoDefaultValue;
    }

    Model transform(ModelTransformer transformer, Model model) {
        // Add clientOptional to:
        // 1. members of @input structures.
        // 2. members that are not required or default.
        // 3. if applyWhenNoDefaultValue, then to members that target structure or union.
        return transformer.mapShapes(model, shape -> {
            if (!(shape instanceof MemberShape)) {
                return shape;
            }

            MemberShape member = (MemberShape) shape;

            // Don't do anything if it's already marked with clientOptional.
            if (member.hasTrait(ClientOptionalTrait.class)) {
                return member;
            }

            Shape container = model.expectShape(member.getContainer());
            Shape target = model.expectShape(member.getTarget());

            boolean hasInputTrait = container.hasTrait(InputTrait.class);
            boolean targetsShapeWithNoZeroValue = target.isStructureShape() || target.isUnionShape();
            boolean isEffectivelyClientOptional = hasInputTrait
                    || !(member.hasTrait(RequiredTrait.class) || member.hasTrait(DefaultTrait.class))
                    || (targetsShapeWithNoZeroValue && applyWhenNoDefaultValue);

            if (isEffectivelyClientOptional) {
                return member.toBuilder().addTrait(new ClientOptionalTrait(member.getSourceLocation())).build();
            }

            return member;
        });
    }
}

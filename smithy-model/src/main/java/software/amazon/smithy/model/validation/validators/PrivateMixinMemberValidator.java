/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Warns when a mixin member is marked with the {@code @private} trait, since
 * the trait does not prevent the member from being inherited by shapes in
 * other namespaces.
 */
public final class PrivateMixinMemberValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : model.getMemberShapesWithTrait(PrivateTrait.class)) {
            Shape container = model.getShape(member.getContainer()).orElse(null);
            if (container != null && container.hasTrait(MixinTrait.ID)) {
                events.add(warning(member,
                        member.expectTrait(PrivateTrait.class),
                        "The `@private` trait does not prevent a mixin member from being inherited by shapes in "
                                + "other namespaces, nor does it prevent new traits from being added to the inherited "
                                + "member. Apply `@private` to the mixin shape or target shape instead if "
                                + "cross-namespace access should be restricted."));
            }
        }
        return events;
    }
}

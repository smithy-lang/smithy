/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates constraints on the mixin trait's properties.
 */
public final class MixinTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(MixinTrait.class)) {
            MixinTrait trait = shape.expectTrait(MixinTrait.class);
            if (trait.isInterface() && shape.isUnionShape()) {
                events.add(error(shape,
                        trait,
                        "The `interface` property of the `@mixin` trait cannot be used on union shapes."));
            }
        }
        return events;
    }
}

/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Finds members marked as sensitive that target shapes marked as sensitive,
 * and find members marked as sensitive that target structures, unions, or
 * enums.
 */
public final class SensitiveTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : model.getMemberShapesWithTrait(SensitiveTrait.class)) {
            SensitiveTrait trait = member.expectTrait(SensitiveTrait.class);
            model.getShape(member.getTarget()).ifPresent(target -> {
                if (target.hasTrait(SensitiveTrait.class)) {
                    events.add(warning(member, trait, "Redundant `sensitive` trait found on member that targets a "
                                                      + "`sensitive` shape"));
                } else if (isBadSensitiveTarget(target)) {
                    events.add(warning(member, trait,
                                       "Members marked with the `sensitive` trait should not target shapes that "
                                       + "represent concrete data types like structures, unions, or enums. A better "
                                       + "approach is to instead mark the targeted shape as sensitive and omit the "
                                       + "`sensitive` trait from the member. This helps to prevent modeling mistakes "
                                       + "by ensuring every reference to concrete data types that are inherently "
                                       + "sensitive are always considered sensitive. Concrete types that are "
                                       + "conditionally sensitive should generally be separated into two types: one "
                                       + "to represent a sensitive type and one to represent the normal type."));
                }
            });
        }
        return events;
    }

    private boolean isBadSensitiveTarget(Shape target) {
        return !(target instanceof SimpleShape) || target.hasTrait(EnumTrait.class);
    }
}

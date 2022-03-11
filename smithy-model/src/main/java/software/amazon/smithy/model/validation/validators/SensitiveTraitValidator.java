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
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a validation event if a model contains members with the sensitive trait.
 */
public final class SensitiveTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : model.getMemberShapesWithTrait(SensitiveTrait.class)) {
            SensitiveTrait trait = member.expectTrait(SensitiveTrait.class);
            events.add(warning(member, trait,
                    "Instead of marking members with the `sensitive` trait, a better approach is to instead mark the "
                    + "targeted shape as sensitive. This helps to prevent modeling mistakes by ensuring every "
                    + "reference to data types that are inherently sensitive are always considered sensitive. Types "
                    + "that are conditionally sensitive should generally be separated into two types: one to "
                    + "represent a sensitive type and one to represent the normal type. Applying `sensitive` trait to"
                    + " members is considered deprecated. It will be removed in Smithy IDL version 2.0."));
        }
        return events;
    }
}

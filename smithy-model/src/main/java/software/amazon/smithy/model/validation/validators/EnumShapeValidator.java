/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits an error validation event if an enum member's enumValue trait has the wrong type,
 * if there are any duplicate values in a single enum, if the enum's default value is
 * set using the enumValue trait, or if an intEnum member lacks an enumValue trait.
 *
 * <p>Additionally, emits warning events when enum member names don't follow the recommended
 * naming convention of all upper case letters separated by underscores.
 */
public final class EnumShapeValidator extends AbstractValidator {
    private static final Pattern RECOMMENDED_NAME_PATTERN = Pattern.compile("^[A-Z]+[A-Z_0-9]*$");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (EnumShape shape : model.getEnumShapes()) {
            validateEnumShape(events, shape);
        }

        for (IntEnumShape shape : model.getIntEnumShapes()) {
            validateIntEnumShape(events, shape);
        }

        return events;
    }

    private void validateEnumShape(List<ValidationEvent> events, EnumShape shape) {
        Set<String> values = new HashSet<>();
        for (MemberShape member : shape.members()) {
            Optional<String> value = member.expectTrait(EnumValueTrait.class).getStringValue();
            if (!value.isPresent()) {
                events.add(error(member, member.expectTrait(EnumValueTrait.class),
                        "The enumValue trait must use the string option when applied to enum shapes."));
            } else {
                if (!values.add(value.get())) {
                    events.add(error(member, String.format(
                            "Multiple enum members found with duplicate value `%s`",
                            value.get()
                    )));
                }
                if (value.get().equals("")) {
                    events.add(error(member, "enum values may not be empty."));
                }
            }
            validateEnumMemberName(events, member);
        }
    }

    private void validateIntEnumShape(List<ValidationEvent> events, IntEnumShape shape) {
        Set<Integer> values = new HashSet<>();
        for (MemberShape member : shape.members()) {
            if (!member.hasTrait(EnumValueTrait.ID)) {
                events.add(missingIntEnumValue(member, member));
            } else if (!member.expectTrait(EnumValueTrait.class).getIntValue().isPresent()) {
                events.add(missingIntEnumValue(member, member.expectTrait(EnumValueTrait.class)));
            } else {
                int value = member.expectTrait(EnumValueTrait.class).getIntValue().get();
                if (!values.add(value)) {
                    events.add(error(member, String.format(
                            "Multiple enum members found with duplicate value `%s`",
                            value
                    )));
                }
            }
            validateEnumMemberName(events, member);
        }
    }

    private ValidationEvent missingIntEnumValue(Shape shape, FromSourceLocation sourceLocation) {
        return error(shape, sourceLocation, "intEnum members must have the enumValue trait with the `int` member set");
    }

    private void validateEnumMemberName(List<ValidationEvent> events, MemberShape member) {
        if (!RECOMMENDED_NAME_PATTERN.matcher(member.getMemberName()).find()) {
            events.add(warning(member, String.format(
                    "The name `%s` does not match the recommended enum name format of beginning with an "
                            + "uppercase letter, followed by any number of uppercase letters, numbers, or underscores.",
                    member.getMemberName())));
        }
    }
}

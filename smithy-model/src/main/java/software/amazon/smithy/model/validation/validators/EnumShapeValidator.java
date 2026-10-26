/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
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
            EnumValueTrait trait = member.expectTrait(EnumValueTrait.class);
            Optional<String> value = trait.getStringValue();
            if (!value.isPresent()) {
                events.add(error(member,
                        member.expectTrait(EnumValueTrait.class),
                        "enum members can only be assigned string values, but found: "
                                + Node.printJson(trait.toNode())));
            } else {
                if (!values.add(value.get())) {
                    events.add(error(member,
                            String.format("Multiple enum members found with duplicate value `%s`",
                                    value.get())));
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
            // intEnum must all have the EnumValueTrait.
            if (!member.hasTrait(EnumValueTrait.ID)) {
                events.add(missingIntEnumValue(member, member));
                continue;
            }

            EnumValueTrait trait = member.expectTrait(EnumValueTrait.class);

            // The EnumValueTrait must point to a number.
            if (!trait.getIntValue().isPresent()) {
                ValidationEvent event = error(member,
                        trait,
                        "intEnum members require integer values, but found: "
                                + Node.printJson(trait.toNode()));
                events.add(event);
                continue;
            }

            NumberNode number = trait.toNode().asNumberNode().get();

            // Validate the it is an integer.
            if (number.isFloatingPointNumber()) {
                events.add(error(member,
                        trait,
                        "intEnum members do not support floating point values: "
                                + number.getValue()));
                continue;
            }

            long longValue = number.getValue().longValue();
            if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
                events.add(error(member,
                        trait,
                        "intEnum members must fit within an integer, but found: "
                                + longValue));
                continue;
            }

            if (!values.add(number.getValue().intValue())) {
                events.add(error(member,
                        String.format("Multiple intEnum members found with duplicate value `%d`",
                                number.getValue().intValue())));
            }

            validateEnumMemberName(events, member);
        }
    }

    private ValidationEvent missingIntEnumValue(Shape shape, FromSourceLocation sourceLocation) {
        return error(shape, sourceLocation, "intEnum members must be assigned an integer value");
    }

    private void validateEnumMemberName(List<ValidationEvent> events, MemberShape member) {
        if (!isRecommendedEnumMemberName(member.getMemberName())) {
            events.add(warning(member,
                    String.format(
                            "The name `%s` does not match the recommended enum name format of beginning with an "
                                    + "uppercase letter, followed by any number of uppercase letters, numbers, or underscores.",
                            member.getMemberName())));
        }
    }

    /**
     * Returns true if the name follows the recommended naming pattern. This method unrolls the
     * "^[A-Z][A-Z_0-9]*$" regex.
     */
    static boolean isRecommendedEnumMemberName(String name) {
        int length = name.length();
        if (length == 0) {
            return false;
        }
        char ch = name.charAt(0);
        if (!isUpperAsciiLetter(ch)) {
            return false;
        }
        for (int i = 1; i < length; i++) {
            ch = name.charAt(i);
            if (isUpperAsciiLetter(ch) || isDigitOrUnderscore(ch)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Returns true if the given char is an upper case ASCII letter.
     */
    static boolean isUpperAsciiLetter(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    /**
     * Returns true if the given char is an underscore or an ASCII digit.
     */
    static boolean isDigitOrUnderscore(char ch) {
        return (ch >= '0' && ch <= '9') || ch == '_';
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a warning validation event if a member shape's name does not follow
 * strict lowerCamelCase naming convention (e.g., "xmlRequest", "fooId").
 *
 * <p>The strict lowerCamelCase pattern requires:
 * <ul>
 *   <li>First character must be lowercase</li>
 *   <li>Subsequent characters can be letters or digits</li>
 *   <li>Uppercase letters are allowed but only as the start of new words</li>
 *   <li>No underscores, hyphens, or other special characters</li>
 * </ul>
 */
public final class MemberNameValidator extends AbstractValidator {
    private static final Pattern STRICT_LOWER_CAMEL_CASE_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (MemberShape member : model.getMemberShapes()) {
            // Skip enum members - they follow UPPER_CASE convention and are validated by EnumShapeValidator
            Shape container = model.expectShape(member.getContainer());
            if (container instanceof EnumShape || container instanceof IntEnumShape) {
                continue;
            }

            validateMemberName(events, member);
        }

        return events;
    }

    private void validateMemberName(List<ValidationEvent> events, MemberShape member) {
        String memberName = member.getMemberName();
        if (!STRICT_LOWER_CAMEL_CASE_PATTERN.matcher(memberName).matches()) {
            events.add(warning(member,
                    String.format(
                            "The member name `%s` does not follow strict lowerCamelCase naming convention. "
                                    + "Member names should start with a lowercase letter and contain only letters and digits "
                                    + "(e.g., \"xmlRequest\", \"fooId\").",
                            memberName)));
        }
    }
}

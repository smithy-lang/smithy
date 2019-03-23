/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.smithy.linters;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Emits a validation event if shapes at a specific location do not match the
 * desired camel casing format.
 *
 * <p>This validator accepts the following optional configuration options:
 *
 * <ul>
 *     <li>shapeNames: (string) one of "upper" (default) or "lower".</li>
 *     <li>memberNames: (string) one of "upper" or "lower" (default).</li>
 * </ul>
 */
public final class CamelCaseValidator extends AbstractValidator {
    private static final String DEFAULT_SHAPE_CASE = "upper";
    private static final String DEFAULT_MEMBER_CASE = "lower";
    private static final Pattern UPPER_CAMEL_CASE = Pattern.compile("^[A-Z]+[A-Za-z0-9]*$");
    private static final Pattern LOWER_CAMEL_CASE = Pattern.compile("^[a-z]+[A-Za-z0-9]*$");

    private final String shapeNames;
    private final String memberNames;

    private CamelCaseValidator(String shapeNames, String memberNames) {
        this.shapeNames = shapeNames;
        this.memberNames = memberNames;
    }

    public static ValidatorService provider() {
        return ValidatorService.createProvider(CamelCaseValidator.class, configuration -> {
            String shapeNames = configuration.getStringMember("shapeName")
                    .orElseGet(() -> Node.from(DEFAULT_SHAPE_CASE))
                    .expectOneOf("upper", "lower");
            String memberNames = configuration.getStringMember("memberNames")
                    .orElseGet(() -> Node.from(DEFAULT_MEMBER_CASE))
                    .expectOneOf("upper", "lower");
            return new CamelCaseValidator(shapeNames, memberNames);
        });
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        Pattern isValidShapeName = getPattern(shapeNames);
        model.getShapeIndex().shapes()
                .filter(shape -> !isValidShapeName.matcher(shape.getId().getName()).find())
                .map(shape -> danger(shape, format("%s shape name, `%s`, is not %s camel case",
                                                   shape.getType(), shape.getId().getName(), shapeNames)))
                .forEach(events::add);

        Pattern isValidMemberName = getPattern(memberNames);
        model.getShapeIndex().shapes(MemberShape.class)
                .filter(shape -> !isValidMemberName.matcher(shape.getMemberName()).find())
                .map(shape -> danger(shape, format("Member shape member name, `%s`, is not %s camel case",
                                                   shape.getMemberName(), memberNames)))
                .forEach(events::add);
        return events;
    }

    private static Pattern getPattern(String upperOrLower) {
        return upperOrLower.equals("upper") ? UPPER_CAMEL_CASE : LOWER_CAMEL_CASE;
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

@Deprecated
public final class StutteredShapeNameValidator extends AbstractValidator {

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(StutteredShapeNameValidator.class, StutteredShapeNameValidator::new);
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        model.shapes(StructureShape.class)
                .forEach(shape -> events.addAll(validateNames(model, shape, shape.getMemberNames())));
        model.shapes(UnionShape.class)
                .forEach(shape -> events.addAll(validateNames(model, shape, shape.getMemberNames())));
        return events;
    }

    private List<ValidationEvent> validateNames(Model model, Shape shape, Collection<String> memberNames) {
        String shapeName = shape.getId().getName();
        String lowerCaseShapeName = shapeName.toLowerCase(Locale.US);
        return memberNames.stream()
                .filter(memberName -> memberName.toLowerCase(Locale.US).startsWith(lowerCaseShapeName))
                .map(memberName -> stutteredMemberName(model, shape, shapeName, memberName))
                .collect(Collectors.toList());
    }

    private ValidationEvent stutteredMemberName(Model model, Shape shape, String shapeName, String memberName) {
        Shape member = model.getShape(shape.getId().withMember(memberName))
                .orElseThrow(
                        () -> new RuntimeException("Invalid member name for shape: " + shape + ", " + memberName));
        return warning(member,
                String.format(
                        "The `%s` %s shape stutters its name in the member `%s`; %2$s member names should not be "
                                + "prefixed with the %2$s name.",
                        shapeName,
                        shape.getType(),
                        memberName));
    }
}

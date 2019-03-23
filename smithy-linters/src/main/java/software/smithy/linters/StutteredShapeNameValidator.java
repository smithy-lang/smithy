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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Validates that structure members and union member names do not
 * stutter their shape name as prefixes of their member or tag names.
 */
public final class StutteredShapeNameValidator extends AbstractValidator {

    public static ValidatorService provider() {
        return ValidatorService.createSimpleProvider(
                StutteredShapeNameValidator.class, StutteredShapeNameValidator::new);
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        var index = model.getShapeIndex();
        var visitor = Shape.<List<ValidationEvent>>visitor()
                .when(UnionShape.class, shape -> validateNames(index, shape, shape.getMemberNames()))
                .when(StructureShape.class, shape -> validateNames(index, shape, shape.getMemberNames()))
                .orElseGet(Collections::emptyList);
        return index.shapes().flatMap(shape -> shape.accept(visitor).stream()).collect(Collectors.toList());
    }

    private List<ValidationEvent> validateNames(ShapeIndex index, Shape shape, Collection<String> memberNames) {
        String shapeName = shape.getId().getName();
        String lowerCaseShapeName = shapeName.toLowerCase(Locale.US);
        return memberNames.stream()
                .filter(memberName -> memberName.toLowerCase(Locale.US).startsWith(lowerCaseShapeName))
                .map(memberName -> stutteredMemberName(index, shape, shapeName, memberName))
                .collect(Collectors.toList());
    }

    private ValidationEvent stutteredMemberName(ShapeIndex index, Shape shape, String shapeName, String memberName) {
        Shape member = index.getShape(shape.getId().withMember(memberName)).orElseThrow(
                () -> new RuntimeException("Invalid member name for shape: " + shape + ", " + memberName));
        return warning(member, String.format(
                "The `%s` %s shape stutters its name in the member `%s`; %2$s member names should not be "
                + "prefixed with the %2$s name.", shapeName, shape.getType(), memberName));
    }
}

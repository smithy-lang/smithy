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

package software.amazon.smithy.model.validation.builtins;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that set members target the appropriate type.
 */
public class SetTargetValidator extends AbstractValidator {
    private static final Set<ShapeType> VALID_TYPES = Set.of(
            ShapeType.STRING, ShapeType.BLOB,
            ShapeType.BYTE, ShapeType.SHORT, ShapeType.INTEGER, ShapeType.LONG,
            ShapeType.BIG_DECIMAL, ShapeType.BIG_INTEGER);

    @Override
    public List<ValidationEvent> validate(Model model) {
        var index = model.getShapeIndex();
        return model.getShapeIndex().shapes(SetShape.class)
                .flatMap(shape -> Pair.flatMapStream(shape, () -> index.getShape(shape.getMember().getTarget())))
                .filter(pair -> !VALID_TYPES.contains(pair.getRight().getType()))
                .map(pair -> error(pair.getLeft(), String.format(
                        "Set member targets an invalid %s shape %s. Set members must target one of the "
                        + "following type: [%s]",
                        pair.getRight().getType(), pair.getRight().getId(), ValidationUtils.tickedList(VALID_TYPES))))
                .collect(Collectors.toList());
    }
}

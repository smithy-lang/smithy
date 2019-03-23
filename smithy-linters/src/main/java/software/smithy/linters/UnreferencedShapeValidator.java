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

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.UnreferencedShapes;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Adds a validation note event for each shape in the shape index that is not
 * connected to a service shape.
 */
public final class UnreferencedShapeValidator extends AbstractValidator {

    public static ValidatorService provider() {
        return ValidatorService.createSimpleProvider(
                UnreferencedShapeValidator.class, UnreferencedShapeValidator::new);
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return new UnreferencedShapes().compute(model).stream()
                .map(shape -> note(shape, String.format(
                        "The %s %s shape is not connected to from any service shape.",
                        shape.getId(), shape.getType())))
                .collect(Collectors.toList());
    }
}

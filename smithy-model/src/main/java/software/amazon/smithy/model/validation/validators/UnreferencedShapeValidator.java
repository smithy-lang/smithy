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

package software.amazon.smithy.model.validation.validators;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.UnreferencedShapes;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Adds a validation note event for each shape in the model that is not
 * connected to a service shape.
 */
public final class UnreferencedShapeValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        long serviceShapes = model.shapes(ServiceShape.class).count();

        // Do not emit validation warnings if no services are present in the model.
        if (serviceShapes == 0) {
            return Collections.emptyList();
        }

        return new UnreferencedShapes().compute(model).stream()
                .map(shape -> note(shape, String.format(
                        "The %s shape is not connected to from any service shape.", shape.getType())))
                .collect(Collectors.toList());
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that service shapes do not contain duplicate resource or
 * operation shape names.
 */
public class ServiceValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        TopDownIndex topDownIndex = model.getKnowledge(TopDownIndex.class);
        return model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(shape -> validateService(topDownIndex, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(TopDownIndex topDownIndex, ServiceShape shape) {
        List<ValidationEvent> events = new ArrayList<>();

        // Ensure that resources bound to the service have unique shape names.
        Map<String, List<ShapeId>> duplicateResourceNames = ValidationUtils.findDuplicateShapeNames(
                topDownIndex.getContainedResources(shape.getId()));
        if (!duplicateResourceNames.isEmpty()) {
            events.add(conflictingNames(shape, "resources", duplicateResourceNames));
        }

        // Ensure that operations bound to the service have unique shape names.
        Map<String, List<ShapeId>> duplicateOperationNames = ValidationUtils.findDuplicateShapeNames(
                topDownIndex.getContainedOperations(shape));
        if (!duplicateOperationNames.isEmpty()) {
            events.add(conflictingNames(shape, "operations", duplicateOperationNames));
        }

        return events;
    }

    private ValidationEvent conflictingNames(ServiceShape shape, String descriptor, Map<String, List<ShapeId>> dupes) {
        return error(shape, String.format(
                "All %s contained within a service hierarchy must have case-insensitively unique names regardless of "
                + "their namespaces. The following %s were found in this service to have conflicting names: %s",
                descriptor, descriptor, dupes));
    }
}

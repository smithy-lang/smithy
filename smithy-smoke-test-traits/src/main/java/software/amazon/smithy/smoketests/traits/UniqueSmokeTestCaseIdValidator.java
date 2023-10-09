/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.smoketests.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that smoke test case IDs are unique across all test cases.
 */
public class UniqueSmokeTestCaseIdValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        Map<String, List<Shape>> testCaseIdsToOperations = new HashMap<>();
        Set<Shape> shapes = model.getShapesWithTrait(SmokeTestsTrait.class);
        for (Shape shape : shapes) {
            SmokeTestsTrait trait = shape.expectTrait(SmokeTestsTrait.class);
            for (SmokeTestCase testCase : trait.getTestCases()) {
                testCaseIdsToOperations.computeIfAbsent(testCase.getId(), id -> new ArrayList<>()).add(shape);
            }
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (Map.Entry<String, List<Shape>> entry : testCaseIdsToOperations.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (Shape shape : entry.getValue()) {
                    events.add(error(shape, String.format(
                            "Conflicting `%s` test case IDs found for ID `%s`: %s",
                            SmokeTestsTrait.ID, entry.getKey(),
                            ValidationUtils.tickedList(entry.getValue().stream().map(Shape::getId))
                    )));
                }
            }
        }
        return events;
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that the "id" property of {@code smithy.test#httpRequestTests},
 * {@code smithy.test#httpResponseTests}, and {@code smithy.test#httpMalformedRequestTests}
 * are unique across all test cases.
 */
@SmithyInternalApi
public class UniqueProtocolTestCaseIdValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        Map<String, List<Shape>> requestIdsToTraits = new TreeMap<>();
        Map<String, List<Shape>> responseIdsToTraits = new TreeMap<>();
        Map<String, List<Shape>> malformedRequestIdsToTraits = new TreeMap<>();

        Stream.concat(model.shapes(OperationShape.class), model.shapes(StructureShape.class)).forEach(shape -> {
            shape.getTrait(HttpRequestTestsTrait.class)
                    .ifPresent(trait -> addTestCaseIdsToMap(shape, trait.getTestCases(), requestIdsToTraits));
            shape.getTrait(HttpResponseTestsTrait.class)
                    .ifPresent(trait -> addTestCaseIdsToMap(shape, trait.getTestCases(), responseIdsToTraits));
            // This deliberately uses the expanded, instead of parameterized test cases,
            // in case someone does something wild with naming, like add _case0 to the end of the id
            shape.getTrait(HttpMalformedRequestTestsTrait.class)
                    .ifPresent(t -> addMalformedRequestTestCaseIdsToMap(shape, t.getTestCases(), responseIdsToTraits));
        });

        removeEntriesWithSingleValue(requestIdsToTraits);
        removeEntriesWithSingleValue(responseIdsToTraits);
        removeEntriesWithSingleValue(malformedRequestIdsToTraits);

        return collectEvents(requestIdsToTraits, responseIdsToTraits, malformedRequestIdsToTraits);
    }

    private void addTestCaseIdsToMap(
            Shape shape,
            List<? extends HttpMessageTestCase> testCases,
            Map<String, List<Shape>> map
    ) {
        for (HttpMessageTestCase testCase : testCases) {
            map.computeIfAbsent(testCase.getId(), id -> new ArrayList<>()).add(shape);
        }
    }

    private void addMalformedRequestTestCaseIdsToMap(
            Shape shape,
            List<HttpMalformedRequestTestCase> testCases,
            Map<String, List<Shape>> map
    ) {
        for (HttpMalformedRequestTestCase testCase : testCases) {
            map.computeIfAbsent(testCase.getId(), id -> new ArrayList<>()).add(shape);
        }
    }

    private void removeEntriesWithSingleValue(Map<String, List<Shape>> map) {
        map.keySet().removeIf(key -> map.get(key).size() == 1);
    }

    private List<ValidationEvent> collectEvents(
            Map<String, List<Shape>> requestIdsToTraits,
            Map<String, List<Shape>> responseIdsToTraits,
            Map<String, List<Shape>> malformedRequestIdsToTraits
    ) {
        if (requestIdsToTraits.isEmpty() && responseIdsToTraits.isEmpty() && malformedRequestIdsToTraits.isEmpty()) {
            return Collections.emptyList();
        }

        List<ValidationEvent> mutableEvents = new ArrayList<>();
        addValidationEvents(requestIdsToTraits, mutableEvents, HttpRequestTestsTrait.ID);
        addValidationEvents(responseIdsToTraits, mutableEvents, HttpResponseTestsTrait.ID);
        addValidationEvents(malformedRequestIdsToTraits, mutableEvents, HttpMalformedRequestTestsTrait.ID);
        return mutableEvents;
    }

    private void addValidationEvents(
            Map<String, List<Shape>> conflicts,
            List<ValidationEvent> mutableEvents,
            ShapeId trait
    ) {
        for (Map.Entry<String, List<Shape>> entry : conflicts.entrySet()) {
            for (Shape shape : entry.getValue()) {
                mutableEvents.add(error(shape,
                        String.format(
                                "Conflicting `%s` test case IDs found for ID `%s`: %s",
                                trait,
                                entry.getKey(),
                                ValidationUtils.tickedList(entry.getValue().stream().map(Shape::getId)))));
            }
        }
    }
}

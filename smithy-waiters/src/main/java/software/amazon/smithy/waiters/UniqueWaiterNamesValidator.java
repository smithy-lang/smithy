/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.waiters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Ensures that no two waiters use the same case-insensitive name in the
 * closure of a service.
 */
@SmithyInternalApi
public final class UniqueWaiterNamesValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ServiceShape.class)
                .flatMap(service -> validateService(model, service).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(Model model, ServiceShape service) {
        Map<String, Set<OperationShape>> waiterNamesToOperations = computeWaiterNamesToOperations(model, service);
        List<ValidationEvent> events = new ArrayList<>();

        for (Map.Entry<String, Set<OperationShape>> entry : waiterNamesToOperations.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (OperationShape operation : entry.getValue()) {
                    Set<ShapeId> conflicts = entry.getValue().stream()
                            .filter(o -> !o.equals(operation))
                            .map(Shape::getId)
                            .collect(Collectors.toCollection(TreeSet::new));
                    events.add(error(operation, operation.expectTrait(WaitableTrait.class), String.format(
                            "`%s` trait waiter name `%s` case-insensitively conflicts with waiters on other "
                            + "operations in the closure of service, `%s`: %s",
                            WaitableTrait.ID,
                            entry.getKey(),
                            service.getId(),
                            conflicts)));
                }
            }
        }

        return events;
    }

    private Map<String, Set<OperationShape>> computeWaiterNamesToOperations(Model model, ServiceShape service) {
        TopDownIndex index = TopDownIndex.of(model);
        Map<String, Set<OperationShape>> waiterNamesToOperations = new TreeMap<>();

        for (OperationShape operation : index.getContainedOperations(service)) {
            operation.getTrait(WaitableTrait.class).ifPresent(trait -> {
                for (String name : trait.getWaiters().keySet()) {
                    Set<OperationShape> operations = waiterNamesToOperations.computeIfAbsent(
                            name.toLowerCase(Locale.ENGLISH), n -> new HashSet<>());
                    operations.add(operation);
                }
            });
        }

        return waiterNamesToOperations;
    }
}

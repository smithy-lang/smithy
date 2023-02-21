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
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates that if any operation in a service uses the http trait,
 * then all operations use them.
 */
public final class HttpBindingsMissingValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(HttpTrait.class)) {
            return Collections.emptyList();
        }

        TopDownIndex topDownIndex = TopDownIndex.of(model);
        return model.shapes(ServiceShape.class)
                .flatMap(shape -> validateService(topDownIndex, model, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(TopDownIndex topDownIndex, Model model, ServiceShape service) {
        Set<OperationShape> operations = topDownIndex.getContainedOperations(service);
        // Stop early if there are no bindings at all in the model for any operation.
        if (operations.stream().noneMatch(this::hasBindings)) {
            return ListUtils.of();
        }

        Severity severity = determineSeverity(service, model);

        return operations.stream()
                .filter(shape -> !shape.getTrait(HttpTrait.class).isPresent())
                .map(shape -> createEvent(severity, service, shape))
                .collect(Collectors.toList());
    }

    // Only emit ERROR events if any of the protocols for the service support HTTP traits
    private Severity determineSeverity(ServiceShape service, Model model) {
        if (ServiceIndex.of(model).getProtocols(service).values().stream()
                .map(t -> model.expectShape(t.toShapeId()))
                .map(s -> s.expectTrait(ProtocolDefinitionTrait.class))
                .anyMatch(pdt -> pdt.getTraits().contains(HttpTrait.ID))) {
            return Severity.ERROR;
        }
        return Severity.WARNING;
    }

    private boolean hasBindings(OperationShape op) {
        return op.getTrait(HttpTrait.class).isPresent();
    }

    private ValidationEvent createEvent(Severity severity, ServiceShape service, OperationShape operation) {
        return createEvent(severity, operation, operation.getSourceLocation(), String.format(
                "One or more operations in the `%s` service define the `http` trait, but this "
                + "operation is missing the `http` trait.", service.getId()));
    }
}

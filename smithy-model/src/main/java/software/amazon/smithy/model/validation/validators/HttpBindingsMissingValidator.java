/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;

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

        ShapeId protocolTraitId = protocolWithBindings(service, model);
        if (protocolTraitId != null) {
            // Emit ERROR events if any of the protocols for the service support HTTP traits.
            String reason = "The `" + protocolTraitId + "` protocol requires all";
            return validateOperations(service, operations, Severity.ERROR, reason);
        }

        // Stop early if there are no bindings at all in the model for any operation.
        if (operations.stream().noneMatch(this::hasBindings)) {
            return ListUtils.of();
        }

        return validateOperations(service, operations, Severity.WARNING, "One or more");
    }

    private ShapeId protocolWithBindings(ServiceShape service, Model model) {
        return ServiceIndex.of(model)
                .getProtocols(service)
                .values()
                .stream()
                .map(t -> model.expectShape(t.toShapeId()))
                .map(s -> Pair.of(s.getId(), s.expectTrait(ProtocolDefinitionTrait.class)))
                .filter(pair -> pair.getRight().getTraits().contains(HttpTrait.ID))
                .map(Pair::getLeft)
                .findFirst()
                .orElse(null);
    }

    private boolean hasBindings(OperationShape op) {
        return op.getTrait(HttpTrait.class).isPresent();
    }

    private List<ValidationEvent> validateOperations(
            ServiceShape service,
            Set<OperationShape> operations,
            Severity severity,
            String reason
    ) {
        return operations.stream()
                .filter(operation -> !operation.getTrait(HttpTrait.class).isPresent())
                .map(operation -> createEvent(severity,
                        operation,
                        operation.getSourceLocation(),
                        String.format("%s operations in the `%s` service define the `http` trait, but this "
                                + "operation is missing the `http` trait.", reason, service.getId())))
                .collect(Collectors.toList());
    }
}

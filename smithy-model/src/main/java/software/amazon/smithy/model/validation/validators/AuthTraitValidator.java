/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * All {@code @auth} trait values referenced from an operation must refer
 * to authentication traits applied to service shapes that enclose the
 * operation.
 */
public final class AuthTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        model.shapes(ServiceShape.class).forEach(service -> validateService(model, service, events));
        return events;
    }

    private void validateService(Model model, ServiceShape service, List<ValidationEvent> events) {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Set<ShapeId> serviceAuth = serviceIndex.getAuthSchemes(service).keySet();
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        // Validate the service's @auth trait.
        validateShape(serviceAuth, service, service, events);

        // Validate each contained operation's @auth trait.
        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            validateShape(serviceAuth, service, operation, events);
        }
    }

    private void validateShape(
            Set<ShapeId> serviceAuth,
            ServiceShape service,
            Shape shape,
            List<ValidationEvent> events
    ) {
        if (shape.getTrait(AuthTrait.class).isPresent()) {
            AuthTrait authTrait = shape.getTrait(AuthTrait.class).get();
            Set<ShapeId> appliedAuthTraitValue = new TreeSet<>(authTrait.getValueSet());
            appliedAuthTraitValue.removeAll(serviceAuth);

            if (!appliedAuthTraitValue.isEmpty()) {
                events.add(danger(shape,
                        authTrait,
                        String.format(
                                "auth trait applies authentication that is not configured on the service shape, `%s`: %s",
                                service.getId(),
                                appliedAuthTraitValue)));
            }
        }
    }
}

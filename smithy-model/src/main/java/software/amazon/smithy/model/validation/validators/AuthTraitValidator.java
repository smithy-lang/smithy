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
public class AuthTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        model.shapes(ServiceShape.class).forEach(service -> validateService(model, service, events));
        return events;
    }

    private void validateService(Model model, ServiceShape service, List<ValidationEvent> events) {
        ServiceIndex serviceIndex = model.getKnowledge(ServiceIndex.class);
        Set<ShapeId> serviceAuth = serviceIndex.getAuthSchemes(service).keySet();
        TopDownIndex topDownIndex = model.getKnowledge(TopDownIndex.class);

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
        if (shape.hasTrait(AuthTrait.class)) {
            AuthTrait authTrait = shape.expectTrait(AuthTrait.class);
            Set<ShapeId> appliedAuthTraitValue = new TreeSet<>(authTrait.getValues());
            appliedAuthTraitValue.removeAll(serviceAuth);

            if (!appliedAuthTraitValue.isEmpty()) {
                events.add(danger(shape, authTrait, String.format(
                        "auth trait applies authentication that is not configured on the service shape, `%s`: %s",
                        service.getId(),
                        appliedAuthTraitValue)));
            }
        }
    }
}

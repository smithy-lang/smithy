/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static software.amazon.smithy.model.validation.ValidationUtils.tickedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Ensures that there is no resource name conflict in a service closure
 * in IAM space after processing {@link IamResourceTrait}.
 */
@SmithyInternalApi
public class IamResourceTraitConflictingNameValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        return model.shapes(ServiceShape.class)
                .flatMap(shape -> validateService(topDownIndex, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(TopDownIndex topDownIndex, ServiceShape service) {
        List<ValidationEvent> events = new ArrayList<>();
        Map<String, List<ShapeId>> resourceMap = new HashMap<>();
        for (ResourceShape resource : topDownIndex.getContainedResources(service)) {
            String resourceName = IamResourceTrait.resolveResourceName(resource);
            resourceMap.computeIfAbsent(resourceName, k -> new ArrayList<>()).add(resource.getId());
        }
        resourceMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> error(service,
                        String.format(
                                "Multiple IAM resources defined with the same IAM resource name is not allowed in a service "
                                        + "closure, but found multiple resources named `%s` in the service `%s`: [%s]",
                                entry.getKey(),
                                service.getId(),
                                tickedList(entry.getValue()))))
                .forEach(events::add);
        return events;
    }
}

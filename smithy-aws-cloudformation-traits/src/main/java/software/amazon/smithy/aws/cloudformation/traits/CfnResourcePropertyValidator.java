/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Validates that derived CloudFormation properties all have the same target.
 */
public final class CfnResourcePropertyValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        CfnResourceIndex cfnResourceIndex = CfnResourceIndex.of(model);
        model.shapes(ResourceShape.class)
                .filter(shape -> shape.hasTrait(CfnResourceTrait.ID))
                .map(shape -> validateResource(model, cfnResourceIndex, shape))
                .forEach(events::addAll);

        return events;
    }

    private List<ValidationEvent> validateResource(
            Model model,
            CfnResourceIndex cfnResourceIndex,
            ResourceShape resource
    ) {
        CfnResourceTrait trait = resource.expectTrait(CfnResourceTrait.class);
        List<ValidationEvent> events = new ArrayList<>();
        String resourceName = trait.getName().orElse(resource.getId().getName());

        cfnResourceIndex.getResource(resource)
                .map(CfnResource::getProperties)
                .ifPresent(properties -> {
                    for (Map.Entry<String, CfnResourceProperty> property : properties.entrySet()) {
                        validateResourceProperty(model, resource, resourceName, property).ifPresent(events::add);
                    }
                });

        return events;
    }

    private Optional<ValidationEvent> validateResourceProperty(
            Model model,
            ResourceShape resource,
            String resourceName,
            Map.Entry<String, CfnResourceProperty> property
    ) {
        Set<ShapeId> propertyTargets = new TreeSet<>();
        for (ShapeId shapeId : property.getValue().getShapeIds()) {
            model.getShape(shapeId).ifPresent(shape ->
            // Use the member target or identifier definition shape.
            OptionalUtils.ifPresentOrElse(shape.asMemberShape(),
                    memberShape -> propertyTargets.add(memberShape.getTarget()),
                    () -> propertyTargets.add(shapeId)));
        }

        if (propertyTargets.size() > 1) {
            return Optional.of(error(resource,
                    String.format("The `%s` property of the generated `%s` "
                            + "CloudFormation resource targets multiple shapes: %s. Reusing member names that "
                            + "target different shapes can cause confusion for users of the API. This target "
                            + "discrepancy must either be resolved in the model or one of the members must be "
                            + "excluded from the conversion.",
                            property.getKey(),
                            resourceName,
                            propertyTargets)));
        }

        return Optional.empty();
    }
}

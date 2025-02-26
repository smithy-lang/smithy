/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
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
        for (ResourceShape resource : model.getResourceShapesWithTrait(CfnResourceTrait.class)) {
            events.addAll(validateResource(model, cfnResourceIndex, resource));
        }

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

        if (trait.getPrimaryIdentifier().isPresent()) {
            validateResourcePrimaryIdentifier(model, resource, trait.getPrimaryIdentifier().get())
                    .ifPresent(events::add);
        }

        Optional<CfnResource> cfnResourceOptional = cfnResourceIndex.getResource(resource);
        if (cfnResourceOptional.isPresent()) {
            for (Map.Entry<String, CfnResourceProperty> property : cfnResourceOptional.get()
                    .getProperties()
                    .entrySet()) {
                validateResourceProperty(model, resource, resourceName, property).ifPresent(events::add);
            }
        }

        return events;
    }

    private Optional<ValidationEvent> validateResourcePrimaryIdentifier(
            Model model,
            ResourceShape resource,
            String primaryIdentifier
    ) {
        Map<String, ShapeId> properties = resource.getProperties();
        if (!properties.containsKey(primaryIdentifier)) {
            return Optional.of(error(resource,
                    resource.expectTrait(CfnResourceTrait.class),
                    format("The alternative resource primary identifier, `%s`, must be a property of the resource.",
                            primaryIdentifier)));
        }

        Shape propertyTarget = model.expectShape(properties.get(primaryIdentifier));
        if (!propertyTarget.isStringShape() && !propertyTarget.isEnumShape()) {
            return Optional.of(error(resource,
                    resource.expectTrait(CfnResourceTrait.class),
                    format("The alternative resource primary identifier, `%s`, targets a `%s` shape, it must target a `string`.",
                            primaryIdentifier,
                            propertyTarget.getType())));
        }

        return Optional.empty();
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
                    format("The `%s` property of the generated `%s` "
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

/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.cloudformation.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates that all shapes referenced by the additionalSchemas property of the
 * {@code @cfnResource} trait are also bound to any service that the resource is
 * bound to.
 */
public final class CfnResourceTraitValidator  extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(CfnResourceTrait.class)) {
            return ListUtils.of();
        }

        List<ValidationEvent> events = new ArrayList<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        Set<ServiceShape> serviceShapes = model.getServiceShapes();

        for (ResourceShape resourceShape : model.getResourceShapesWithTrait(CfnResourceTrait.class)) {
            CfnResourceTrait cfnResourceTrait = resourceShape.expectTrait(CfnResourceTrait.class);

            // Short-circuit if there are no additionalSchemas for this resource.
            if (cfnResourceTrait.getAdditionalSchemas().isEmpty()) {
                continue;
            }

            // Validate that the for each service that contains the resource.
            for (ServiceShape serviceShape : serviceShapes) {
                if (!topDownIndex.getContainedResources(serviceShape).contains(resourceShape)) {
                    continue;
                }

                events.addAll(validateAdditionalSchemaBindings(model, serviceShape, resourceShape, cfnResourceTrait));
            }
        }

        return events;
    }

    private List<ValidationEvent> validateAdditionalSchemaBindings(
            Model model,
            ServiceShape service,
            ResourceShape resource,
            CfnResourceTrait trait
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        for (ShapeId additionalSchemaId : trait.getAdditionalSchemas()) {
            Selector selector = Selector.parse("[id = " + additionalSchemaId + "]");

            // Emit an event if we don't have a path to the additionalSchema from the
            // service shape this resource is bound to.
            if (PathFinder.create(model).search(service.getId(), selector).isEmpty()) {
                events.add(error(resource, trait.getSourceLocation(), String.format("The `%s` structure listed as "
                        + "an `additionalSchema` in the `cfnResource` trait on this resource must be bound to the "
                        + "`%s` service that binds this resource.", additionalSchemaId, service.getId())));
            }
        }

        return events;
    }
}

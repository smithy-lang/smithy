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

package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

public final class UnreferencedEntityValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        Walker shapeWalker = new Walker(model.getKnowledge(NeighborProviderIndex.class).getProvider());
        Set<ServiceShape> serviceShapes = model.shapes(ServiceShape.class)
                //.filter(serviceShape -> serviceShape.getId().getNamespace().equals("ns.special"))
                .collect(Collectors.toSet());

        // Do not emit validation warnings if no services are present in the model.
        if (serviceShapes.isEmpty()) {
            return Collections.emptyList();
        }

        // Gather all shapes connected to a service in a model.
        Set<Shape> serviceConnected = serviceShapes.stream()
                .flatMap(service -> shapeWalker.walkShapes(service).stream())
                .collect(Collectors.toSet());

        // Gather all shapes connected to a resource in a model.
        Set<Shape> resourceConnected = model.shapes(ResourceShape.class)
                .flatMap(resource -> shapeWalker.walkShapes(resource).stream())
                .collect(Collectors.toSet());

        // Gather operations not connected to a service or resource.
        Set<Shape> unreferenced = model.shapes()
                .filter(Shape::isOperationShape)
                .filter(shape -> !(serviceConnected.contains(shape) || resourceConnected.contains(shape)))
                .collect(Collectors.toSet());

        // Add resources that are not referenced by a service or other resources.
        model.shapes()
                .filter(Shape::isResourceShape)
                .filter(resource -> !serviceConnected.contains(resource))
                .forEach(resource -> {
                    if (!isReferencedByAnotherResource(resource, model)) {
                        unreferenced.add(resource);
                    }
                });

        return unreferenced.stream()
                .map(shape -> warning(shape, format("This %s shape is not bound to a service", shape.getType())))
                .collect(Collectors.toList());
    }

    // Checks if a shape is referenced by any other resources in a model.
    private boolean isReferencedByAnotherResource(Shape shape, Model model) {
        Walker shapeWalker = new Walker(model.getKnowledge(NeighborProviderIndex.class).getProvider());
        Set<Shape> connectedToAnotherResource = model.shapes(ResourceShape.class)
                .filter(otherResource -> !otherResource.equals(shape))
                .flatMap(resource -> shapeWalker.walkShapes(resource).stream())
                .collect(Collectors.toSet());
        return connectedToAnotherResource.contains(shape);
    }
}

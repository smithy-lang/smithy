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

package software.amazon.smithy.aws.traits.endpointdiscovery;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;

/**
 * Removes the endpoint discovery trait from a service if the referenced operation or error are removed.
 */
public class CleanDiscoveryTraitTransformer implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> shapes, Model model) {
        Set<ShapeId> removedOperations = shapes.stream()
                .filter(Shape::isOperationShape)
                .map(Shape::getId)
                .collect(Collectors.toSet());
        Set<ShapeId> removedErrors = shapes.stream()
                .filter(shape -> shape.hasTrait(ErrorTrait.class))
                .map(Shape::getId)
                .collect(Collectors.toSet());

        Set<Shape> toReplace = getServicesToUpdate(model, removedOperations, removedErrors);
        return transformer.replaceShapes(model, toReplace);
    }

    private Set<Shape> getServicesToUpdate(Model model, Set<ShapeId> removedOperations, Set<ShapeId> removedErrors) {
        return model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(service -> Trait.flatMapStream(service, EndpointDiscoveryTrait.class))
                .filter(pair -> removedOperations.contains(pair.getRight().getOperation())
                        || removedErrors.contains(pair.getRight().getError()))
                .map(pair -> {
                    ServiceShape.Builder builder = pair.getLeft().toBuilder();
                    builder.removeTrait(EndpointDiscoveryTrait.ID);
                    return builder.build();
                })
                .collect(Collectors.toSet());
    }
}

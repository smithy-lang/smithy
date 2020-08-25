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

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures that shapes in separate namespaces don't refer to shapes in other
 * namespaces that are marked as private.
 */
public final class PrivateAccessValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        Set<Shape> privateShapes = model.getShapesWithTrait(PrivateTrait.class);
        NeighborProvider provider = NeighborProviderIndex.of(model).getProvider();
        return model.shapes()
                .filter(shape -> !(shape instanceof SimpleShape))
                .flatMap(shape -> validateNeighbors(shape, provider.getNeighbors(shape), privateShapes))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateNeighbors(
            Shape shape,
            List<Relationship> relationships,
            Set<Shape> privateShapes
    ) {
        return relationships.stream()
                .filter(rel -> privateShapes.contains(rel.getNeighborShape().orElse(null)))
                .filter(rel -> !rel.getNeighborShapeId().getNamespace().equals(shape.getId().getNamespace()))
                .map(rel -> error(shape, String.format(
                        "This shape has an invalid %s relationship that targets a private shape, `%s`, in "
                        + "another namespace.",
                        rel.getRelationshipType().toString().toLowerCase(Locale.US),
                        rel.getNeighborShapeId())));
    }
}

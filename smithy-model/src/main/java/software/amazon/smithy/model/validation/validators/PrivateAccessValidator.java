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
import java.util.Locale;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
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
        NeighborProvider provider = NeighborProviderIndex.of(model).getReverseProviderWithTraitRelationships();

        List<ValidationEvent> events = new ArrayList<>();
        for (Shape privateShape : model.getShapesWithTrait(PrivateTrait.class)) {
            validateNeighbors(privateShape, provider.getNeighbors(privateShape), events);
        }

        return events;
    }

    private void validateNeighbors(Shape shape, List<Relationship> relationships, List<ValidationEvent> events) {
        String namespace = shape.getId().getNamespace();
        for (Relationship rel : relationships) {
            if (!rel.getShape().getId().getNamespace().equals(namespace)) {
                events.add(getPrivateAccessValidationEvent(rel));
            }
        }
    }

    private ValidationEvent getPrivateAccessValidationEvent(Relationship relationship) {
        ShapeId neighborId = relationship.expectNeighborShape().getId();
        String message = String.format(
                "This shape has an invalid %s relationship that targets a private shape, `%s`, in another namespace.",
                relationship.getRelationshipType().toString().toLowerCase(Locale.US),
                neighborId
        );

        // For now, emit a warning for trait relationships instead of an error. This is because private access on trait
        // relationships was not being validated in the past, so emitting a warning maintains backward compatibility.
        // This will be upgraded to an error in the future.
        if (relationship.getRelationshipType().equals(RelationshipType.TRAIT)) {
            return warning(relationship.getShape(), message);
        } else {
            return error(relationship.getShape(), message);
        }
    }
}

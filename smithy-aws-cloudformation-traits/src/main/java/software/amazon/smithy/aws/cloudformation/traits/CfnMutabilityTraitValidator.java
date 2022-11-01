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

package software.amazon.smithy.aws.cloudformation.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that members marked as having write-only mutability are not also
 * marked as additional identifiers for their CloudFormation resource.
 */
public final class CfnMutabilityTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Shape shape : model.getShapesWithTrait(CfnMutabilityTrait.class)) {
            CfnMutabilityTrait trait = shape.expectTrait(CfnMutabilityTrait.class);
            // Additional identifiers must be able to be read, so write and
            // create mutabilities cannot overlap.
            if (shape.hasTrait(CfnAdditionalIdentifierTrait.ID) && (trait.isWrite() || trait.isCreate())) {
                events.add(error(shape, trait, String.format("Member with the mutability value of \"%s\" "
                        + "is also marked as an additional identifier", trait.getValue())));
            }

            // Must check for unsafe usage when overriding mutability for identifier bindings
            for (Shape resourceShape : model.getShapesWithTrait(CfnResourceTrait.class)) {
                CfnResourceTrait resourceTrait = resourceShape.expectTrait(CfnResourceTrait.class);
                model.getShape(ShapeId.fromParts(shape.getId().getNamespace(), shape.getId().getName())).ifPresent(
                        schemaShape -> {
                            boolean isIdentifier = false;
                            // check that the resource schemas include the shape, and that the member
                            //  is bound as an identifier in the resource
                            if (resourceTrait.getAdditionalSchemas().contains(schemaShape.toShapeId())) {
                                isIdentifier = resourceShape.asResourceShape()
                                        .map(ResourceShape::getIdentifiers)
                                        .orElseGet(Collections::emptyMap)
                                        .containsKey(
                                                shape.asMemberShape()
                                                        .map(MemberShape::getMemberName)
                                                        .orElseGet(String::new)
                                        );
                            }
                            // If member is implicitly an identifier, validate its mutability
                            if (isIdentifier && !(trait.isRead() || trait.isCreateAndRead())) {
                                events.add(error(shape, trait, String.format(
                                        "Member is implicitly bound as an identifier \"%s\" and "
                                                + "cannot have \"%s\" mutability", shape.getId(), trait.getValue())));
                            }
                });
            }
        }

        return events;
    }
}

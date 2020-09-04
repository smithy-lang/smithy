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
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
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
        }

        return events;
    }
}

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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates that trait values are valid for their trait definitions.
 */
public final class TraitValueValidator implements Validator {

    public static final String VALIDATE_PRELUDE = "__validatePrelude__";
    private static final String NAME = "TraitValue";

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        boolean validatePrelude = model.getMetadataProperty(VALIDATE_PRELUDE).isPresent();
        for (Shape shape : model.toSet()) {
            for (Trait trait : shape.getAllTraits().values()) {
                events.addAll(validateTrait(model, shape, trait, validatePrelude));
            }
        }

        return events;
    }

    private List<ValidationEvent> validateTrait(
            Model model,
            Shape targetShape,
            Trait trait,
            boolean validatePrelude
    ) {
        ShapeId shape = trait.toShapeId();

        if (!model.getShape(shape).isPresent()) {
            // Punt; invalid ID targets are validated in TraitDefinitionShapeValidator.
            return ListUtils.of();
        }

        if (!validatePrelude && Prelude.isPreludeShape(targetShape)) {
            // The prelude is validated through tests in smithy-model and does not
            // need to be validated here since traits can't be applied to shapes in
            // the prelude outside of the prelude.
            return ListUtils.of();
        }

        Shape schema = model.getShape(shape).get();
        NodeValidationVisitor cases = NodeValidationVisitor.builder()
                .model(model)
                .value(trait.toNode())
                .eventShapeId(targetShape.getId())
                .eventId(NAME)
                .startingContext("Error validating trait `" + Trait.getIdiomaticTraitName(trait) + "`")
                .build();

        return schema.accept(cases);
    }
}

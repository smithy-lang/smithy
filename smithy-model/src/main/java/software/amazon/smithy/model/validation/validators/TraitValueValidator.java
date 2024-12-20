/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
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
        // Create a reusable validation visitor so that the
        // selector cache is shared for each trait.
        NodeValidationVisitor validator = NodeValidationVisitor.builder()
                .eventId(NAME)
                .model(model)
                .value(Node.nullNode())
                .build();

        List<ValidationEvent> events = new ArrayList<>();
        boolean validatePrelude = model.getMetadataProperty(VALIDATE_PRELUDE).isPresent();
        for (Shape shape : model.toSet()) {
            for (Trait trait : shape.getAllTraits().values()) {
                events.addAll(validateTrait(model, validator, shape, trait, validatePrelude));
            }
        }

        return events;
    }

    private List<ValidationEvent> validateTrait(
            Model model,
            NodeValidationVisitor validator,
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

        validator.setValue(trait.toNode());
        validator.setEventShapeId(targetShape.getId());
        validator.setStartingContext("Error validating trait `" + Trait.getIdiomaticTraitName(trait) + "`");
        return model.getShape(shape).get().accept(validator);
    }
}

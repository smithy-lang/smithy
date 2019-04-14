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
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.Triple;

/**
 * Validates that trait values are valid for their trait definitions.
 */
public final class TraitValueValidator implements Validator {

    private static final String NAME = "TraitValue";

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.getShapeIndex()
                .shapes()
                // Get pairs of <Shape, Trait>
                .flatMap(shape -> shape.getAllTraits().values().stream().map(t -> Pair.of(shape, t)))
                // Get a triple of Shape, Trait, TraitDefinition.
                .flatMap(pair -> OptionalUtils.stream(model.getTraitDefinition(pair.getRight().getName())
                        .map(traitDefinition -> Triple.fromPair(pair, traitDefinition))))
                .flatMap(triple -> validateTrait(model.getShapeIndex(), triple.a, triple.b, triple.c).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateTrait(
            ShapeIndex index,
            Shape targetShape,
            Trait trait,
            TraitDefinition definition
    ) {
        if (definition.isAnnotationTrait()) {
            if (trait.toNode().isBooleanNode() && trait.toNode().expectBooleanNode().getValue()) {
                return ListUtils.of();
            }

            return ListUtils.of(ValidationEvent.builder()
                    .severity(Severity.ERROR)
                    .eventId(NAME)
                    .sourceLocation(trait)
                    .shapeId(targetShape.getId())
                    .message("Value provided for boolean trait `%s` can only be set to true. "
                             + "Found %s", Trait.getIdiomaticTraitName(trait.getName()), trait.toNode().getType())
                    .build());
        }

        ShapeId shape = definition.getShape().get();
        if (!index.getShape(shape).isPresent()) {
            // This is validated in TraitDefinitionShapeValidator.
            return ListUtils.of();
        }

        Shape schema = index.getShape(definition.getShape().get()).get();
        NodeValidationVisitor cases = NodeValidationVisitor.builder()
                .index(index)
                .value(trait.toNode())
                .eventShapeId(targetShape.getId())
                .eventId(NAME)
                .startingContext("Error validating trait `" + Trait.getIdiomaticTraitName(trait.getName()) + "`")
                .build();

        return schema.accept(cases);
    }
}

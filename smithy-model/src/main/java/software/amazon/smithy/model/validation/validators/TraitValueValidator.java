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
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Validates that trait values are valid for their trait definitions.
 */
public final class TraitValueValidator implements Validator {

    private static final String NAME = "TraitValue";

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes()
                // Get pairs of <Shape, Trait>
                .flatMap(shape -> shape.getAllTraits().values().stream().map(t -> Pair.of(shape, t)))
                .flatMap(pair -> validateTrait(model, pair.left, pair.right).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateTrait(Model model, Shape targetShape, Trait trait) {
        ShapeId shape = trait.toShapeId();

        if (!model.getShape(shape).isPresent()) {
            // Punt; invalid ID targets are validated in TraitDefinitionShapeValidator.
            return ListUtils.of();
        }

        Shape schema = model.expectShape(shape);
        Node coerced = Trait.coerceTraitValue(trait.toNode(), schema.getType());

        NodeValidationVisitor cases = NodeValidationVisitor.builder()
                .model(model)
                .value(coerced)
                .eventShapeId(targetShape.getId())
                .eventId(NAME)
                .startingContext("Error validating trait `" + Trait.getIdiomaticTraitName(trait) + "`")
                .build();

        return schema.accept(cases);
    }
}

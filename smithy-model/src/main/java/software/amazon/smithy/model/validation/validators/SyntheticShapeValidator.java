/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.synthetic.SyntheticShapeTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that synthetic shapes do not have any user-applied traits,
 * and warns if user-defined shapes use the reserved {@code _Synthetic} prefix.
 */
@SmithyInternalApi
public final class SyntheticShapeValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<Shape> syntheticShapes = model.getShapesWithTrait(SyntheticShapeTrait.ID);

        for (Shape shape : syntheticShapes) {
            for (Trait trait : shape.getAllTraits().values()) {
                if (!trait.toShapeId().equals(SyntheticShapeTrait.ID)) {
                    events.add(error(shape,
                            trait,
                            "Traits cannot be applied to synthetic shapes. "
                                    + "Found trait `" + Trait.getIdiomaticTraitName(trait.toShapeId())
                                    + "` on assembler-generated shape `" + shape.getId() + "`."));
                }
            }
        }

        for (Shape shape : model.toSet()) {
            if (!shape.hasTrait(SyntheticShapeTrait.ID)
                    && shape.getId().getName().startsWith("_Synthetic")) {
                events.add(warning(shape,
                        "Shape name starts with `_Synthetic` which is reserved for "
                                + "assembler-generated shapes. Consider renaming this shape "
                                + "to avoid potential conflicts."));
            }
        }

        return events;
    }
}

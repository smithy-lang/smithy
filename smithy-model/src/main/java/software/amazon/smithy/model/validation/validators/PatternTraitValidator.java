/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.StringJoiner;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a validation event if a pattern trait is not anchored.
 */
public final class PatternTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(PatternTrait.class)) {
            validatePatternTrait(events, shape);
        }

        return events;
    }

    private void validatePatternTrait(List<ValidationEvent> events, Shape shape) {
        PatternTrait trait = shape.expectTrait(PatternTrait.class);
        String pattern = trait.getValue();
        boolean leading = pattern.startsWith("^");
        boolean trailing = pattern.endsWith("$");
        if (!leading || !trailing) {
            StringJoiner sj = new StringJoiner(" and ");
            if (!leading) {
                sj.add("leading '^'");
            }
            if (!trailing) {
                sj.add("trailing '$'");
            }
            events.add(warning(shape, trait, String.format(
                    "A pattern trait is applied without a %s, meaning only part of the string must match the regular "
                    + "expression. Explicitly anchoring regular expressions is preferable because it is more "
                    + "restrictive by default and does not require modelers to understand that Smithy patterns are "
                    + "not automatically anchored.", sj)));
        }
    }
}

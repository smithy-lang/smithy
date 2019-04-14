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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumConstantBody;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures that enum traits are valid.
 */
public class EnumTraitValidator extends AbstractValidator {
    private static final Pattern RECOMMENDED_NAME_PATTERN = Pattern.compile("^[A-Z]+[A-Z_0-9]*$");

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.getShapeIndex().shapes(StringShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, EnumTrait.class))
                .flatMap(pair -> validateEnumTrait(pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateEnumTrait(Shape shape, EnumTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Map.Entry<String, EnumConstantBody> entry : trait.getValues().entrySet()) {
            if (entry.getValue().getName().isPresent()) {
                String name = entry.getValue().getName().get();
                if (names.contains(name)) {
                    events.add(error(shape, trait, String.format(
                            "Duplicate enum trait values found with the same `name` property of '%s'", name)));
                }
                if (!RECOMMENDED_NAME_PATTERN.matcher(name).find()) {
                    events.add(warning(shape, trait, String.format(
                            "The name `%s` does not match our recommended enum name format of beginning with an "
                            + "uppercase letter, followed by any number of uppercase letters, numbers, or underscores.",
                            name)));
                }
                names.add(name);
            }
        }

        if (!names.isEmpty()) {
            for (Map.Entry<String, EnumConstantBody> entry : trait.getValues().entrySet()) {
                if (!entry.getValue().getName().isPresent()) {
                    events.add(error(shape, trait, String.format(
                            "`%s` enum value body is missing the `name` property; if any enum trait value contains a "
                            + "`name` property, then all values must contain the `name` property.", entry.getKey())));
                }
            }
        }

        return events;
    }
}

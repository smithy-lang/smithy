/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures that enum traits are valid.
 *
 * <p>If one enum definition contains a name, then all definitions must contain
 * a name. All enum values and names must be unique across the list of
 * definitions.
 */
public final class EnumTraitValidator extends AbstractValidator {
    private static final Pattern RECOMMENDED_NAME_PATTERN = Pattern.compile("^[A-Z]+[A-Z_0-9]*$");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Shape shape : model.getShapesWithTrait(EnumTrait.class)) {
            events.addAll(validateEnumTrait(shape, shape.expectTrait(EnumTrait.class)));
        }

        return events;
    }

    private List<ValidationEvent> validateEnumTrait(Shape shape, EnumTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> values = new HashSet<>();

        // Ensure that values are unique.
        for (EnumDefinition definition : trait.getValues()) {
            if (!values.add(definition.getValue())) {
                events.add(error(shape,
                        trait,
                        String.format(
                                "Duplicate enum trait values found with the same `value` property of '%s'",
                                definition.getValue())));
            }
        }

        // Ensure that names are unique.
        for (EnumDefinition definition : trait.getValues()) {
            if (definition.getName().isPresent()) {
                String name = definition.getName().get();
                if (!names.add(name)) {
                    events.add(error(shape,
                            trait,
                            String.format(
                                    "Duplicate enum trait values found with the same `name` property of '%s'",
                                    name)));
                }
                if (!RECOMMENDED_NAME_PATTERN.matcher(name).find()) {
                    events.add(warning(
                            shape,
                            trait,
                            String.format("The name `%s` does not match the recommended enum name format of beginning "
                                    + "with an uppercase letter, followed by any number of uppercase letters, numbers, "
                                    + "or underscores.", name),
                            name));
                }
            }
        }

        if (!names.isEmpty()) {
            // If one enum definition has a name, then they all must have names.
            for (EnumDefinition definition : trait.getValues()) {
                if (!definition.getName().isPresent()) {
                    events.add(error(shape,
                            trait,
                            String.format(
                                    "`%s` enum value body is missing the `name` property; if any enum trait value contains a "
                                            + "`name` property, then all values must contain the `name` property.",
                                    definition.getValue())));
                }
            }
        } else {
            // Enums SHOULD have names, so warn if there are none.
            ValidationEvent event = warning(shape,
                    trait,
                    "Enums should define the `name` property to allow rich "
                            + "types to be generated in code generators.");
            // Change the id of the event so that it can be suppressed separately.
            events.add(event.toBuilder().id("EnumNamesPresent").build());
        }

        return events;
    }
}

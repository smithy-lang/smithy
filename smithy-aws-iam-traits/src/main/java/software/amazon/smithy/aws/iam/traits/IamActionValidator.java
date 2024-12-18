/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class IamActionValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapesWithTrait(IamActionTrait.class)) {
            IamActionTrait trait = operation.expectTrait(IamActionTrait.class);
            events.addAll(validateDuplicateTraits(operation, trait));
            validateUniqueResourceNames(operation, trait).ifPresent(events::add);
        }
        return events;
    }

    @SuppressWarnings("deprecation")
    private List<ValidationEvent> validateDuplicateTraits(OperationShape operation, IamActionTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();
        if (operation.hasTrait(ActionNameTrait.ID) && trait.getName().isPresent()) {
            events.add(emitDeprecatedOverride(operation,
                    operation.expectTrait(ActionNameTrait.class),
                    "name"));
        }

        if (operation.hasTrait(ActionPermissionDescriptionTrait.ID) && trait.getDocumentation().isPresent()) {
            events.add(emitDeprecatedOverride(operation,
                    operation.expectTrait(ActionPermissionDescriptionTrait.class),
                    "documentation"));
        }

        if (operation.hasTrait(RequiredActionsTrait.ID) && !trait.getRequiredActions().isEmpty()) {
            events.add(emitDeprecatedOverride(operation,
                    operation.expectTrait(RequiredActionsTrait.class),
                    "requiredActions"));
        }
        return events;
    }

    private ValidationEvent emitDeprecatedOverride(OperationShape operation, Trait trait, String name) {
        return error(operation,
                trait,
                format("Operation has the `%s` property of the "
                        + "`@aws.iam#iamAction` trait set and the deprecated `@%s` trait applied.",
                        name,
                        trait.toShapeId()),
                "ConflictingProperty",
                name);
    }

    private Optional<ValidationEvent> validateUniqueResourceNames(OperationShape operation, IamActionTrait trait) {
        if (!trait.getResources().isPresent()
                || trait.getResources().get().getRequired().isEmpty()
                || trait.getResources().get().getOptional().isEmpty()) {
            return Optional.empty();
        }

        Set<String> duplicateNames = new LinkedHashSet<>(trait.getResources().get().getRequired().keySet());
        duplicateNames.retainAll(trait.getResources().get().getOptional().keySet());
        if (duplicateNames.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(danger(operation,
                trait,
                "Operation has the following resource names defined as both "
                        + "required and optional: " + duplicateNames,
                "Resources",
                "DuplicateEntries"));
    }
}

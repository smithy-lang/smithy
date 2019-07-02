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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Validates traits that can only be applied to a single structure member.
 */
public class ExclusiveStructureMemberTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.getShapeIndex().shapes(StructureShape.class)
                .flatMap(shape -> validateExclusiveTraits(model, shape).stream())
                .collect(toList());
    }

    private List<ValidationEvent> validateExclusiveTraits(Model model, StructureShape shape) {
        // Find all members marked as structurally exclusive, and foreach,
        // ensure that multiple members are not marked with the trait.
        return shape.getAllMembers().values().stream()
                .flatMap(member -> member.getAllTraits().values().stream())
                .filter(trait -> isExclusive(model, trait))
                .flatMap(t -> OptionalUtils.stream(validateExclusiveTrait(shape, t.getTraitName())))
                .collect(Collectors.toList());
    }

    private boolean isExclusive(Model model, Trait trait) {
        return model.getTraitDefinition(trait.getTraitName())
                .map(TraitDefinition::isStructurallyExclusive)
                .orElse(false);
    }

    private Optional<ValidationEvent> validateExclusiveTrait(StructureShape shape, String traitName) {
        List<String> matches = shape.getAllMembers().values().stream()
                .filter(member -> member.findTrait(traitName).isPresent())
                .map(MemberShape::getMemberName)
                .collect(Collectors.toList());

        if (matches.size() > 1) {
            return Optional.of(error(shape, String.format(
                    "The `%s` trait can be applied to only a single member of a structure, but was found on "
                    + "the following members: %s",
                    Trait.getIdiomaticTraitName(traitName), ValidationUtils.tickedList(matches))));
        }

        return Optional.empty();
    }
}

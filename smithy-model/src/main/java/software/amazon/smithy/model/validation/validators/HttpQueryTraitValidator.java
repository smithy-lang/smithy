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

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that httpQuery trait bindings are case-sensitively unique.
 */
public final class HttpQueryTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(StructureShape.class)
                .flatMap(shape -> validateStructure(shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateStructure(StructureShape structure) {
        return structure.getAllMembers().values().stream()
                .flatMap(member -> Trait.flatMapStream(member, HttpQueryTrait.class))
                .collect(Collectors.groupingBy(pair -> pair.getRight().getValue(),
                                               mapping(pair -> pair.getLeft().getMemberName(), toList())))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> error(structure, String.format(
                        "`httpQuery` parameter name binding conflicts found for the `%s` parameter in the "
                        + "following structure members: %s",
                        entry.getKey(), ValidationUtils.tickedList(entry.getValue()))))
                .collect(toList());
    }
}

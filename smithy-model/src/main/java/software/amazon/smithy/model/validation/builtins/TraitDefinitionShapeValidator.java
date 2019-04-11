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

package software.amazon.smithy.model.validation.builtins;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Validates that custom trait shapes target valid shapes.
 */
public final class TraitDefinitionShapeValidator implements Validator {

    private static final String NAME = "TraitDefinitionShape";

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex index = model.getShapeIndex();
        return model.getTraitDefinitions()
                .stream()
                .filter(FunctionalUtils.not(TraitDefinition::isAnnotationTrait))
                .flatMap(definition -> Pair.flatMapStream(definition, TraitDefinition::getShape))
                .filter(pair -> index.getShape(pair.getRight()).isEmpty())
                .map(pair -> ValidationEvent.builder()
                        .severity(Severity.ERROR)
                        .eventId(NAME)
                        .sourceLocation(pair.getLeft().getSourceLocation())
                        .message("Trait definition `%s` targets an unresolvable shape `%s`",
                                 pair.getLeft().getFullyQualifiedName(), pair.getRight())
                        .build())
                .collect(Collectors.toList());
    }
}

/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SetUtils;

public class StreamingTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        Set<ShapeId> topLevelIoShapes = model.shapes(OperationShape.class)
                .flatMap(operation -> SetUtils.of(operation.getInput(), operation.getOutput()).stream())
                .flatMap(OptionalUtils::stream)
                .collect(Collectors.toSet());

        return model.shapes(MemberShape.class)
                .filter(member -> member.getMemberTrait(model, StreamingTrait.class).isPresent())
                .filter(member -> !topLevelIoShapes.contains(member.getContainer()))
                .map(member -> error(member, String.format(
                        "The shape %s has the smithy.api#streaming trait, and so may only be targeted by "
                        + "top-level operation inputs and outputs.",
                        member.getTarget())))
                .collect(Collectors.toList());
    }
}

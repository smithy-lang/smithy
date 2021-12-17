/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.diff.evaluators;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * When removing the required trait, it has to be replaced with the default
 * trait, unless the containing structure is marked with the input trait.
 */
public class RemovedRequiredTrait extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(MemberShape.class)
                .map(change -> {
                    MemberShape oldShape = change.getOldShape();
                    MemberShape newShape = change.getNewShape();
                    if (oldShape.hasTrait(RequiredTrait.class)
                            && !newShape.hasTrait(RequiredTrait.class)
                            && !newShape.hasTrait(DefaultTrait.class)
                            && !containerHasInputTrait(differences.getNewModel(), newShape)) {
                        return error(newShape, "Removed the @required trait without replacing it with the @default "
                                               + "trait. Code generated for this structure will change in a backward "
                                               + "incompatible way in many languages, including Rust, Kotlin, Swift, "
                                               + "and many others.");
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean containerHasInputTrait(Model model, MemberShape member) {
        return model.getShape(member.getContainer())
                .filter(container -> container.hasTrait(InputTrait.class))
                .isPresent();
    }
}

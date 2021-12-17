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
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * The default trait can only be added to shape if it's replacing the
 * required trait.
 */
public class AddedDefaultTrait extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(MemberShape.class)
                .map(change -> {
                    MemberShape oldShape = change.getOldShape();
                    MemberShape newShape = change.getNewShape();
                    if (newShape.hasTrait(DefaultTrait.class)
                            && !oldShape.hasTrait(DefaultTrait.class)
                            && !oldShape.hasTrait(RequiredTrait.class)) {
                        return error(newShape, "Added the @default trait. This is only backward compatible if "
                                               + "the @default trait is used to replace the @required trait.");
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

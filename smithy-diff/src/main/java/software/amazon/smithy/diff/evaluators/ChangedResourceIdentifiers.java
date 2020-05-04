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

package software.amazon.smithy.diff.evaluators;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates an ERROR event when the identifiers of a resource changes.
 */
public final class ChangedResourceIdentifiers extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(ResourceShape.class)
                .filter(diff -> !diff.getOldShape().getIdentifiers().equals(diff.getNewShape().getIdentifiers()))
                .map(diff -> error(diff.getNewShape(), String.format(
                        "Identifiers of resource `%s` changed from %s to %s.",
                        diff.getShapeId(), diff.getOldShape().getIdentifiers(), diff.getNewShape().getIdentifiers())))
                .collect(Collectors.toList());
    }
}

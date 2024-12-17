/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
                .map(diff -> error(diff.getNewShape(),
                        String.format(
                                "Identifiers of resource `%s` changed from %s to %s.",
                                diff.getShapeId(),
                                diff.getOldShape().getIdentifiers(),
                                diff.getNewShape().getIdentifiers())))
                .collect(Collectors.toList());
    }
}

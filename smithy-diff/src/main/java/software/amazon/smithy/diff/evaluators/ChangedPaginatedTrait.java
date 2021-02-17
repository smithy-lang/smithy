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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a DANGER when a backward incompatible change is made to the
 * paginated trait.
 */
public final class ChangedPaginatedTrait extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes()
                .flatMap(change -> validateChange(change).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateChange(ChangedShape<Shape> change) {
        return change.getChangedTrait(PaginatedTrait.class)
                .map(pair -> validateChange(change.getNewShape(), pair.left, pair.right))
                .orElse(Collections.emptyList());
    }

    private List<ValidationEvent> validateChange(Shape shape, PaginatedTrait left, PaginatedTrait right) {
        List<ValidationEvent> events = new ArrayList<>();

        // Items cannot be added, removed, or changed.
        if (!left.getItems().equals(right.getItems())) {
            events.add(danger(shape, right, String.format(
                    "The items property of the `paginated` trait was changed from %s to %s.",
                    formatValue(left.getItems().orElse(null)),
                    formatValue(right.getItems().orElse(null)))));
        }

        // pageSize can be added, but not removed or changed.
        if (!left.getPageSize().equals(right.getPageSize()) && left.getPageSize().isPresent()) {
            events.add(danger(shape, right, String.format(
                    "The pageSize property of the `paginated` trait was changed from %s to %s.",
                    formatValue(left.getPageSize().orElse(null)),
                    formatValue(right.getPageSize().orElse(null)))));
        }

        // inputToken cannot be changed.
        if (!left.getInputToken().equals(right.getInputToken())) {
            events.add(danger(shape, right, String.format(
                    "The inputToken property of the `paginated` trait was changed from %s to %s. In rare cases, "
                    + "this change can be backward compatible if every binding of this operation to a service is "
                    + "to a service that defines a partial pagination trait that uses the same inputToken member.",
                    formatValue(left.getInputToken().orElse(null)),
                    formatValue(right.getInputToken().orElse(null)))));
        }

        // outputToken cannot be changed.
        if (!left.getOutputToken().equals(right.getOutputToken())) {
            events.add(danger(shape, right, String.format(
                    "The outputToken property of the `paginated` trait was changed from %s to %s. In rare cases, "
                    + "this change can be backward compatible if every binding of this operation to a service is "
                    + "to a service that defines a partial pagination trait that uses the same outputToken member.",
                    formatValue(left.getOutputToken().orElse(null)),
                    formatValue(right.getOutputToken().orElse(null)))));
        }

        return events;
    }

    private static String formatValue(String value) {
        if (value == null) {
            return "none";
        } else {
            return '\'' + value + '\'';
        }
    }
}

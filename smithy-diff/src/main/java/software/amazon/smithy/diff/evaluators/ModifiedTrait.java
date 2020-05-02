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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Finds breaking changes related to when a trait is added, removed, or
 * updated.
 *
 * <p>This evaluator looks for trait definitions with specific tags. When
 * traits that use these tags are added, removed, or updated, a validation
 * event is emitted for the change. This evaluator honors the following tags:
 *
 * <ul>
 *     <li>diff.error.add: It is an error to add a trait to an existing shape.</li>
 *     <li>diff.error.remove: It is an error to remove this trait from a shape.</li>
 *     <li>diff.error.update: It is an error to change the value of this shape.</li>
 *     <li>diff.error.const: It is an error to add, remove, or change a trait.</li>
 * </ul>
 */
public class ModifiedTrait extends AbstractDiffEvaluator {
    /** Tags that indicates a breaking change if a trait is added. */
    public static final String DIFF_ERROR_ADD = "diff.error.add";

    /** Tags that indicates a breaking change if a trait is removed. */
    public static final String DIFF_ERROR_REMOVE = "diff.error.remove";

    /** Tags that indicates a breaking change if a trait is updated. */
    public static final String DIFF_ERROR_UPDATE = "diff.error.update";

    /** Tags that indicates a breaking change if a trait is added, removed, or changed. */
    public static final String DIFF_ERROR_CONST = "diff.error.const";

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        Model newModel = differences.getNewModel();

        // Find trait definitions that are tagged with any of the known diff tags.
        List<ValidationEvent> events = new ArrayList<>();
        Set<ShapeId> errorOnAdd = findMatchingTraitDefNamesWithTag(newModel, DIFF_ERROR_ADD);
        Set<ShapeId> errorOnRemove = findMatchingTraitDefNamesWithTag(newModel, DIFF_ERROR_REMOVE);
        Set<ShapeId> errorOnReplace = findMatchingTraitDefNamesWithTag(newModel, DIFF_ERROR_UPDATE);

        // Merge "diff.error.const" into the other categories.
        Set<ShapeId> errorOnAddOrRemove = findMatchingTraitDefNamesWithTag(newModel, DIFF_ERROR_CONST);
        errorOnAdd.addAll(errorOnAddOrRemove);
        errorOnRemove.addAll(errorOnAddOrRemove);
        errorOnReplace.addAll(errorOnAddOrRemove);

        differences.changedShapes().forEach(changedShape -> {
            changedShape.getTraitDifferences().forEach((traitName, pair) -> {
                Trait oldTrait = pair.getLeft();
                Trait newTrait = pair.getRight();
                if (errorOnAdd.contains(traitName) && oldTrait == null) {
                    events.add(error(changedShape.getNewShape(), newTrait, String.format(
                            "It is a breaking change to add the `%s` trait to the existing `%s` %s shape. "
                            + "The added trait value is: %s",
                            traitName,
                            changedShape.getNewShape().getId(),
                            changedShape.getNewShape().getType(),
                            Node.prettyPrintJson(newTrait.toNode()))));
                } else if (errorOnRemove.contains(traitName) && newTrait == null) {
                    events.add(error(changedShape.getNewShape(), String.format(
                            "It is a breaking change to remove the `%s` trait from the `%s` %s shape. "
                            + "The removed trait value was: %s",
                            traitName,
                            changedShape.getNewShape().getId(),
                            changedShape.getNewShape().getType(),
                            Node.prettyPrintJson(oldTrait.toNode()))));
                } else if (errorOnReplace.contains(traitName)) {
                    events.add(error(changedShape.getNewShape(), newTrait, String.format(
                            "It is a breaking change to change the value of the `%s` trait on the `%s` %s shape. "
                            + "The old trait value was: %s. The new trait value is: %s",
                            traitName,
                            changedShape.getNewShape().getId(),
                            changedShape.getNewShape().getType(),
                            Node.prettyPrintJson(oldTrait.toNode()),
                            Node.prettyPrintJson(newTrait.toNode()))));
                }
            });
        });

        return events;
    }

    private static Set<ShapeId> findMatchingTraitDefNamesWithTag(Model model, String tag) {
        return model.getShapesWithTrait(TraitDefinition.class).stream()
                .filter(def -> def.getTags().contains(tag))
                .map(Shape::getId)
                .collect(Collectors.toSet());
    }
}

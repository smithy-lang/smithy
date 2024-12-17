/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.AddedDefaultTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class ChangedDefault extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        List<ValidationEvent> events = new ArrayList<>();

        // Find changes in the DefaultTrait.
        differences.changedShapes().forEach(change -> {
            change.getTraitDifferences().forEach((traitId, pair) -> {
                if (traitId.equals(DefaultTrait.ID)) {
                    if ((pair.left == null || pair.left instanceof DefaultTrait)
                            && (pair.right == null || pair.right instanceof DefaultTrait)) {
                        validateChange(events,
                                differences.getNewModel(),
                                change,
                                (DefaultTrait) pair.left,
                                (DefaultTrait) pair.right);
                    }
                }
            });
        });

        return events;
    }

    private void validateChange(
            List<ValidationEvent> events,
            Model model,
            ChangedShape<Shape> change,
            DefaultTrait oldTrait,
            DefaultTrait newTrait
    ) {
        if (newTrait == null) {
            if (!isInconsequentialRemovalOfDefaultTrait(model, oldTrait, change.getNewShape())) {
                events.add(error(change.getNewShape(),
                        "@default trait was removed. This will break previously generated code."));
            }
        } else if (oldTrait == null) {
            if (change.getNewShape().getType() != ShapeType.MEMBER) {
                events.add(error(change.getNewShape(),
                        newTrait,
                        "Adding the @default trait to a root-level shape will break previously generated "
                                + "code. Added @default: " + Node.printJson(newTrait.toNode())));
            } else if (!change.getNewShape().hasTrait(AddedDefaultTrait.class)) {
                if (!newTrait.toNode().isNullNode()) {
                    events.add(error(change.getNewShape(),
                            newTrait,
                            "Adding the @default trait to a member without also adding the @addedDefault "
                                    + "trait will break previously generated code. Added @default: "
                                    + Node.printJson(newTrait.toNode())));
                }
            }
        } else if (!oldTrait.toNode().equals(newTrait.toNode())) {
            if (change.getNewShape().isMemberShape()) {
                evaluateChangedTrait(model, change.getNewShape().asMemberShape().get(), oldTrait, newTrait, events);
            } else {
                events.add(error(change.getNewShape(),
                        newTrait,
                        "Changing the @default value of a root-level shape will break previously generated "
                                + "code. Old value: " + Node.printJson(oldTrait.toNode())
                                + ". New value: " + Node.printJson(newTrait.toNode())));
            }
        }
    }

    private boolean isInconsequentialRemovalOfDefaultTrait(Model model, DefaultTrait trait, Shape removedFrom) {
        // Removing a default of null if the target is nullable is not an issue.
        return removedFrom.asMemberShape()
                .map(member -> {
                    if (trait.toNode().isNullNode()) {
                        // If the target has no defined default, then removing a default(null) trait is fine.
                        Node targetDefault = model.expectShape(member.getTarget())
                                .getTrait(DefaultTrait.class)
                                .map(DefaultTrait::toNode)
                                .orElse(Node.nullNode()); // If no default, then assume target has a default of null.
                        return targetDefault.isNullNode();
                    } else {
                        // Removing a non-null trait is always an issue.
                        return false;
                    }
                })
                .orElse(false);
    }

    private void evaluateChangedTrait(
            Model model,
            MemberShape member,
            DefaultTrait oldTrait,
            DefaultTrait newTrait,
            List<ValidationEvent> events
    ) {
        Shape target = model.expectShape(member.getTarget());
        Node oldValue = oldTrait.toNode();
        Node newValue = newTrait.toNode();
        boolean oldZeroValue = NullableIndex.isDefaultZeroValueOfTypeInV1(oldValue, target.getType());
        boolean newZeroValue = NullableIndex.isDefaultZeroValueOfTypeInV1(newValue, target.getType());

        if (oldZeroValue == newZeroValue) {
            events.add(danger(member,
                    newTrait,
                    "Changing the @default value of a member is dangerous and could break "
                            + "previously generated code or lead to subtle errors. Do this only "
                            + "when strictly necessary. Old value: " + Node.printJson(oldValue)
                            + ". New value: " + Node.printJson(newValue)));
        } else if (oldZeroValue) {
            events.add(error(member,
                    newTrait,
                    "The @default trait of this member changed from the zero value of the "
                            + "target shape, `" + Node.printJson(oldValue) + "`, to a value that "
                            + "is not the zero value, `" + Node.printJson(newValue) + "`. This "
                            + "will break previously generated code."));
        } else {
            events.add(error(member,
                    newTrait,
                    "The @default trait of this member changed from something other than "
                            + "the zero value of the target shape, `" + Node.printJson(oldValue)
                            + "`, to the zero value, `" + Node.printJson(newValue) + "`. This "
                            + "will break previously generated code."));
        }
    }
}

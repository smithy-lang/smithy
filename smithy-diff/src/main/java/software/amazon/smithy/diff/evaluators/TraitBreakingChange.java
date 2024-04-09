/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * Finds breaking changes related to when a trait is added, removed, or
 * updated based on the breakingChanges property of traits.
 */
public final class TraitBreakingChange extends AbstractDiffEvaluator {

    private static final List<TraitDefinition.ChangeType> ANY_TYPES = Arrays.asList(
            TraitDefinition.ChangeType.ADD,
            TraitDefinition.ChangeType.REMOVE,
            TraitDefinition.ChangeType.UPDATE);

    private static final List<TraitDefinition.ChangeType> PRESENCE_TYPES = Arrays.asList(
            TraitDefinition.ChangeType.ADD,
            TraitDefinition.ChangeType.REMOVE);

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        List<ValidationEvent> events = new ArrayList<>();

        differences.changedShapes().forEach(changedShape -> {
            changedShape.getTraitDifferences().forEach((traitId, oldTraitNewTraitPair) -> {
                Trait oldTrait = oldTraitNewTraitPair.left;
                Trait newTrait = oldTraitNewTraitPair.right;
                // Use the breaking changes rules of the new trait.
                differences.getNewModel().getShape(traitId).ifPresent(traitShape -> {
                    List<TraitDefinition.BreakingChangeRule> rules = traitShape
                            .expectTrait(TraitDefinition.class)
                            .getBreakingChanges();
                    for (TraitDefinition.BreakingChangeRule rule : rules) {
                        PathChecker checker = new PathChecker(differences.getNewModel(), traitShape,
                                                              changedShape.getNewShape(), rule, events);
                        checker.check(Node.from(oldTrait), Node.from(newTrait));
                    }
                });
            });
        });

        return events;
    }

    private static final class PathChecker {
        private final Model model;
        private final Shape trait;
        private final Shape targetShape;
        private final TraitDefinition.BreakingChangeRule rule;
        private final List<ValidationEvent> events;
        private final List<String> segements;

        PathChecker(
                Model model,
                Shape trait,
                Shape targetShape,
                TraitDefinition.BreakingChangeRule rule,
                List<ValidationEvent> events
        ) {
            this.model = model;
            this.trait = trait;
            this.targetShape = targetShape;
            this.rule = rule;
            this.events = events;
            this.segements = rule.getDefaultedPath().getParts();
        }

        private void check(Node left, Node right) {
            // Only perform nested diffs if the right node was found.
            if (right.isNullNode() && !segements.isEmpty()) {
                return;
            }

            Map<String, Node> leftValues = new TreeMap<>();
            Map<String, Node> rightValues = new TreeMap<>();
            extract(leftValues, trait, 0, left, "");
            extract(rightValues, trait, 0, right, "");

            // Compare values that exist only in left or in both.
            for (Map.Entry<String, Node> entry : leftValues.entrySet()) {
                Node rightValue = rightValues.getOrDefault(entry.getKey(), Node.nullNode());
                compareResult(entry.getKey(), entry.getValue(), rightValue);
            }

            // Find newly added values.
            for (Map.Entry<String, Node> entry : rightValues.entrySet()) {
                if (!leftValues.containsKey(entry.getKey())) {
                    compareResult(entry.getKey(), Node.nullNode(), entry.getValue());
                }
            }
        }

        private void extract(
                Map<String, Node> result,
                Shape currentShape,
                int segmentPosition,
                Node currentValue,
                String path
        ) {
            // Don't keep crawling when a "" segment is hit or the last segment is hit.
            if (segmentPosition >= segements.size() || segements.get(segmentPosition).isEmpty()) {
                result.put(path, currentValue);
                return;
            }

            String segment = segements.get(segmentPosition);
            currentShape.getMember(segment).flatMap(m -> model.getShape(m.getTarget())).ifPresent(nextShape -> {
                if (currentShape instanceof CollectionShape) {
                    currentValue.asArrayNode().ifPresent(v -> {
                        for (int i = 0; i < v.size(); i++) {
                            Node value = v.get(i).get();
                            extract(result, nextShape, segmentPosition + 1, value, path + "/" + i);
                        }
                    });
                } else if (currentShape instanceof MapShape) {
                    currentValue.asObjectNode().ifPresent(v -> {
                        for (Map.Entry<String, Node> entry : v.getStringMap().entrySet()) {
                            extract(result, nextShape, segmentPosition + 1,
                                    entry.getValue(), path + "/" + entry.getKey());
                        }
                    });
                } else if (currentShape.isStructureShape() || currentShape.isUnionShape()) {
                    currentValue.asObjectNode().ifPresent(v -> {
                        extract(result, nextShape, segmentPosition + 1, v.getMember(segment).orElse(Node.nullNode()),
                                path + "/" + segment);
                    });
                }
            });
        }

        private void compareResult(String path, Node left, Node right) {
            if (!left.isNullNode() || !right.isNullNode()) {
                TraitDefinition.ChangeType type = isChangeBreaking(rule.getChange(), left, right);
                if (type != null) {
                    String message = createBreakingMessage(type, path, left, right);
                    if (rule.getMessage().isPresent()) {
                        if (!message.endsWith(".")) {
                            message = message + "; ";
                        }
                        message = message + rule.getMessage().get();
                    }
                    FromSourceLocation location = !right.isNullNode() ? right : targetShape;
                    events.add(ValidationEvent.builder()
                                       .id(getValidationEventId(type))
                                       .severity(rule.getDefaultedSeverity())
                                       .shape(targetShape)
                                       .sourceLocation(location)
                                       .message(message)
                                       .build());
                }
            }
        }

        private String getValidationEventId(TraitDefinition.ChangeType type) {
            return String.format("%s.%s.%s", TraitBreakingChange.class.getSimpleName(),
                    StringUtils.capitalize(type.toString()), trait.getId());
        }

        // Check if a breaking change was encountered, and return the type of breaking change.
        private TraitDefinition.ChangeType isChangeBreaking(TraitDefinition.ChangeType type, Node left, Node right) {
            switch (type) {
                case ADD:
                    return left.isNullNode() && !right.isNullNode() ? type : null;
                case REMOVE:
                    return right.isNullNode() && !left.isNullNode() ? type : null;
                case UPDATE:
                    return !left.isNullNode() && !right.isNullNode() && !left.equals(right) ? type : null;
                case ANY:
                    for (TraitDefinition.ChangeType checkType : ANY_TYPES) {
                        if (isChangeBreaking(checkType, left, right) != null) {
                            return checkType;
                        }
                    }
                    return null;
                case PRESENCE:
                    for (TraitDefinition.ChangeType checkType : PRESENCE_TYPES) {
                        if (isChangeBreaking(checkType, left, right) != null) {
                            return checkType;
                        }
                    }
                default:
                    return null;
            }
        }

        private String createBreakingMessage(TraitDefinition.ChangeType type, String path, Node left, Node right) {
            String leftPretty = ValidationUtils.tickedPrettyPrintedNode(left);
            String rightPretty = ValidationUtils.tickedPrettyPrintedNode(right);

            switch (type) {
                case ADD:
                    if (!path.isEmpty()) {
                        return String.format("Added trait contents to `%s` at path `%s` with value %s",
                                             trait.getId(), path, rightPretty);
                    } else if (Node.objectNode().equals(right)) {
                        return String.format("Added trait `%s`", trait.getId());
                    } else {
                        return String.format("Added trait `%s` with value %s", trait.getId(), rightPretty);
                    }
                case REMOVE:
                    if (!path.isEmpty()) {
                        return String.format("Removed trait contents from `%s` at path `%s`. Removed value: %s",
                                             trait.getId(), path, leftPretty);
                    } else if (Node.objectNode().equals(left)) {
                        return String.format("Removed trait `%s`", trait.getId());
                    } else {
                        return String.format("Removed trait `%s`. Previous trait value: %s",
                                             trait.getId(), leftPretty);
                    }
                case UPDATE:
                    if (!path.isEmpty()) {
                        return String.format("Changed trait contents of `%s` at path `%s` from %s to %s",
                                             trait.getId(), path, leftPretty, rightPretty);
                    } else {
                        return String.format("Changed trait `%s` from %s to %s",
                                             trait.getId(), leftPretty, rightPretty);
                    }
                default:
                    throw new UnsupportedOperationException("Expected add, remove, update: " + type);
            }
        }
    }
}

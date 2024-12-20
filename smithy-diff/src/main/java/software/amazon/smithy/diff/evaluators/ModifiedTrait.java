/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * Finds breaking changes related to when a trait is added, removed, or
 * updated.
 *
 * <p>Note that the use of special diff tags is deprecated in favor of using
 * the breakingChanges property of a trait definition. See
 * {@link TraitBreakingChange}.
 *
 * <p>This evaluator looks for trait definitions with specific tags. When
 * traits that use these tags are added, removed, or updated, a validation
 * event is emitted for the change. This uses honors the following tags:
 *
 * <ul>
 *     <li>diff.error.add: It is an error to add a trait to an existing shape or to add
 *     a member to a nested trait value.</li>
 *     <li>diff.error.remove: It is an error to remove this trait from a shape or to
 *     remove a member from a nested trait value.</li>
 *     <li>diff.error.update: It is an error to change the value of this shape
 *     or a member of a nested trait value.</li>
 *     <li>diff.error.const: It is an error to add, remove, or update a trait or
 *     a member of a nested trait value.</li>
 *     <li>diff.danger.add: It is a danger to add a trait to an existing shape or to add
 *     a member to a nested trait value.</li>
 *     <li>diff.danger.remove: It is a danger to remove this trait from a shape or to
 *     remove a member from a nested trait value.</li>
 *     <li>diff.danger.update: It is a danger to change the value of this shape
 *     or a member of a nested trait value.</li>
 *     <li>diff.danger.const: It is a danger to add, remove, or update a trait or
 *     a member of a nested trait value.</li>
 *     <li>diff.warning.add: It is a warning to add a trait to an existing shape or to add
 *     a member to a nested trait value.</li>
 *     <li>diff.warning.remove: It is a warning to remove this trait from a shape or to
 *     remove a member from a nested trait value.</li>
 *     <li>diff.warning.update: It is a warning to change the value of this shape
 *     or a member of a nested trait value.</li>
 *     <li>diff.warning.const: It is a warning to add, remove, or update a trait or
 *     a member of a nested trait value.</li>
 *     <li>diff.contents: Inspect the nested contents of a trait using diff tags.</li>
 * </ul>
 */
public final class ModifiedTrait extends AbstractDiffEvaluator {

    /**
     * Traits that aren't tagged with diff.*.[add|remove|update|const] use a
     * default set of diff strategies so we are notified when traits are modified.
     */
    private static final List<DiffStrategy> DEFAULT_STRATEGIES = ListUtils.of(
            new DiffStrategy(DiffType.ADD, Severity.NOTE),
            new DiffStrategy(DiffType.UPDATE, Severity.NOTE),
            new DiffStrategy(DiffType.REMOVE, Severity.WARNING));

    /** Traits in this list have special backward compatibility rules and can't be validated here. */
    private static final Set<ShapeId> IGNORED_TRAITS = SetUtils.of(BoxTrait.ID,
            RequiredTrait.ID,
            SyntheticEnumTrait.ID,
            OriginalShapeIdTrait.ID);

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        // Map of trait shape ID to diff strategies to evaluate.
        Map<ShapeId, List<DiffStrategy>> strategies = computeDiffStrategies(differences.getNewModel());
        List<ValidationEvent> events = new ArrayList<>();

        differences.changedShapes().forEach(changedShape -> {
            changedShape.getTraitDifferences().forEach((traitId, oldTraitNewTraitPair) -> {
                Trait oldTrait = oldTraitNewTraitPair.left;
                Trait newTrait = oldTraitNewTraitPair.right;
                // Do not emit for the box trait because it is added and removed for backward compatibility.
                if (!IGNORED_TRAITS.contains(traitId)) {
                    // If we don't know about the trait, warn on any change to it.
                    List<DiffStrategy> diffStrategies = strategies.computeIfAbsent(traitId,
                            t -> ListUtils.of(new DiffStrategy(DiffType.CONST, Severity.WARNING)));

                    for (DiffStrategy strategy : diffStrategies) {
                        List<ValidationEvent> diffEvents = strategy.diffType.validate(
                                differences.getNewModel(),
                                "",
                                changedShape.getNewShape(),
                                traitId,
                                oldTrait == null ? null : oldTrait.toNode(),
                                newTrait == null ? null : newTrait.toNode(),
                                strategy.severity);
                        events.addAll(diffEvents);
                    }
                }
            });
        });

        return events;
    }

    private static Map<ShapeId, List<DiffStrategy>> computeDiffStrategies(Model model) {
        Map<ShapeId, List<DiffStrategy>> result = new HashMap<>();

        // Find all trait definition shapes.
        for (Shape shape : model.getShapesWithTrait(TraitDefinition.class)) {
            TraitDefinition definition = shape.expectTrait(TraitDefinition.class);
            List<DiffStrategy> strategies = createStrategiesForShape(shape, true);
            if (!strategies.isEmpty()) {
                result.put(shape.getId(), strategies);
            } else if (!definition.getBreakingChanges().isEmpty()) {
                // Avoid duplicate validation events; delegate emitting events to TraitBreakingChange.
                result.put(shape.getId(), Collections.emptyList());
            } else {
                result.put(shape.getId(), DEFAULT_STRATEGIES);
            }
        }

        return result;
    }

    private static List<DiffStrategy> createStrategiesForShape(Shape shape, boolean allowContents) {
        List<DiffStrategy> strategies = new ArrayList<>();

        for (String tag : shape.getTags()) {
            DiffStrategy value = DiffStrategy.fromTag(tag, allowContents);
            if (value != null) {
                strategies.add(value);
            }
        }

        return strategies;
    }

    private static final class DiffStrategy {
        private final DiffType diffType;
        private final Severity severity;

        DiffStrategy(DiffType diffType, Severity severity) {
            this.diffType = diffType;
            this.severity = severity;
        }

        private static DiffStrategy fromTag(String tag, boolean allowContents) {
            switch (tag) {
                case "diff.contents":
                    return allowContents ? new DiffStrategy(DiffType.CONTENTS, null) : null;
                case "diff.error.add":
                    return new DiffStrategy(DiffType.ADD, Severity.ERROR);
                case "diff.error.remove":
                    return new DiffStrategy(DiffType.REMOVE, Severity.ERROR);
                case "diff.error.update":
                    return new DiffStrategy(DiffType.UPDATE, Severity.ERROR);
                case "diff.error.const":
                    return new DiffStrategy(DiffType.CONST, Severity.ERROR);
                case "diff.danger.add":
                    return new DiffStrategy(DiffType.ADD, Severity.DANGER);
                case "diff.danger.remove":
                    return new DiffStrategy(DiffType.REMOVE, Severity.DANGER);
                case "diff.danger.update":
                    return new DiffStrategy(DiffType.UPDATE, Severity.DANGER);
                case "diff.danger.const":
                    return new DiffStrategy(DiffType.CONST, Severity.DANGER);
                case "diff.warning.add":
                    return new DiffStrategy(DiffType.ADD, Severity.WARNING);
                case "diff.warning.remove":
                    return new DiffStrategy(DiffType.REMOVE, Severity.WARNING);
                case "diff.warning.update":
                    return new DiffStrategy(DiffType.UPDATE, Severity.WARNING);
                case "diff.warning.const":
                    return new DiffStrategy(DiffType.CONST, Severity.WARNING);
                default:
                    // Skip non-diff tags.
                    return null;
            }
        }
    }

    private enum DiffType {
        ADD {
            @Override
            List<ValidationEvent> validate(
                    Model model,
                    String path,
                    Shape shape,
                    ShapeId trait,
                    Node left,
                    Node right,
                    Severity severity
            ) {
                if (left != null) {
                    return Collections.emptyList();
                }

                String message;
                String pretty = ValidationUtils.tickedPrettyPrintedNode(right);
                if (path.isEmpty()) {
                    message = String.format("Added trait `%s` with value %s", trait, pretty);
                } else {
                    message = String.format("Added trait contents to `%s` at path `%s` with value %s",
                            trait,
                            path,
                            pretty);
                }

                return Collections.singletonList(ValidationEvent.builder()
                        .id(getValidationEventId(this, trait))
                        .severity(severity)
                        .shape(shape)
                        .sourceLocation(right)
                        .message(message)
                        .build());
            }
        },

        REMOVE {
            @Override
            List<ValidationEvent> validate(
                    Model model,
                    String path,
                    Shape shape,
                    ShapeId trait,
                    Node left,
                    Node right,
                    Severity severity
            ) {
                if (right != null) {
                    return Collections.emptyList();
                }

                String pretty = ValidationUtils.tickedPrettyPrintedNode(left);
                String message;
                if (path.isEmpty()) {
                    message = String.format("Removed trait `%s`. Previous trait value: %s", trait, pretty);
                } else {
                    message = String.format("Removed trait contents from `%s` at path `%s`. Removed value: %s",
                            trait,
                            path,
                            pretty);
                }

                return Collections.singletonList(ValidationEvent.builder()
                        .id(getValidationEventId(this, trait))
                        .severity(severity)
                        .shape(shape)
                        .sourceLocation(left.getSourceLocation())
                        .message(message)
                        .build());
            }
        },

        UPDATE {
            @Override
            List<ValidationEvent> validate(
                    Model model,
                    String path,
                    Shape shape,
                    ShapeId trait,
                    Node left,
                    Node right,
                    Severity severity
            ) {
                if (left == null || right == null || Objects.equals(left, right)) {
                    return Collections.emptyList();
                }

                String leftPretty = ValidationUtils.tickedPrettyPrintedNode(left);
                String rightPretty = ValidationUtils.tickedPrettyPrintedNode(right);
                String message;
                if (path.isEmpty()) {
                    message = String.format("Changed trait `%s` from %s to %s", trait, leftPretty, rightPretty);
                } else {
                    message = String.format("Changed trait contents of `%s` at path `%s` from %s to %s",
                            trait,
                            path,
                            leftPretty,
                            rightPretty);
                }

                return Collections.singletonList(ValidationEvent.builder()
                        .id(getValidationEventId(this, trait))
                        .severity(severity)
                        .shape(shape)
                        .message(message)
                        .build());
            }
        },

        CONST {
            @Override
            List<ValidationEvent> validate(
                    Model model,
                    String path,
                    Shape shape,
                    ShapeId trait,
                    Node left,
                    Node right,
                    Severity severity
            ) {
                List<ValidationEvent> events = new ArrayList<>();
                events.addAll(ADD.validate(model, path, shape, trait, left, right, severity));
                events.addAll(REMOVE.validate(model, path, shape, trait, left, right, severity));
                events.addAll(UPDATE.validate(model, path, shape, trait, left, right, severity));
                return events;
            }
        },

        CONTENTS {
            @Override
            List<ValidationEvent> validate(
                    Model model,
                    String path,
                    Shape shape,
                    ShapeId trait,
                    Node left,
                    Node right,
                    Severity severity
            ) {
                // The trait needs to exist in both models to perform this check.
                if (left == null || right == null) {
                    return Collections.emptyList();
                }

                Shape traitShape = model.getShape(trait).orElse(null);

                // Defer to other validators in the rare case the trait isn't defined in the model.
                if (traitShape == null) {
                    return Collections.emptyList();
                }

                List<ValidationEvent> events = new ArrayList<>();
                crawlContents(model, shape, trait, traitShape, left, right, events, "");

                return events;
            }
        };

        abstract List<ValidationEvent> validate(
                Model model,
                String path,
                Shape shape,
                ShapeId trait,
                Node left,
                Node right,
                Severity severity
        );

        private static String getValidationEventId(DiffType diffType, ShapeId trait) {
            return String.format("%s.%s.%s",
                    ModifiedTrait.class.getSimpleName(),
                    StringUtils.capitalize(StringUtils.lowerCase(diffType.toString())),
                    trait);
        }
    }

    private static void crawlContents(
            Model model,
            Shape startingShape,
            ShapeId trait,
            Shape currentTraitShape,
            Node leftValue,
            Node rightValue,
            List<ValidationEvent> events,
            String path
    ) {
        currentTraitShape.accept(new DiffCrawler(model, startingShape, trait, leftValue, rightValue, events, path));
    }

    private static final class DiffCrawler extends ShapeVisitor.Default<Void> {

        private final Model model;
        private final Shape startingShape;
        private final ShapeId trait;
        private final Node leftValue;
        private final Node rightValue;
        private final List<ValidationEvent> events;
        private final String path;

        DiffCrawler(
                Model model,
                Shape startingShape,
                ShapeId trait,
                Node leftValue,
                Node rightValue,
                List<ValidationEvent> events,
                String path
        ) {
            this.model = model;
            this.startingShape = startingShape;
            this.trait = trait;
            this.leftValue = leftValue;
            this.rightValue = rightValue;
            this.events = events;
            this.path = path;
        }

        @Override
        public Void listShape(ListShape shape) {
            if (leftValue != null && rightValue != null && leftValue.isArrayNode() && rightValue.isArrayNode()) {
                List<Node> leftValues = leftValue.expectArrayNode().getElements();
                List<Node> rightValues = rightValue.expectArrayNode().getElements();

                // Look for changed and removed elements.
                for (int i = 0; i < leftValues.size(); i++) {
                    Node element = leftValues.get(i);
                    if (rightValues.size() > i) {
                        crawlContents(model,
                                startingShape,
                                trait,
                                shape.getMember(),
                                element,
                                rightValues.get(i),
                                events,
                                path + '/' + i);
                    } else {
                        crawlContents(model,
                                startingShape,
                                trait,
                                shape.getMember(),
                                element,
                                null,
                                events,
                                path + '/' + i);
                    }
                }

                // Look for added elements.
                for (int i = 0; i < rightValues.size(); i++) {
                    Node element = rightValues.get(i);
                    if (leftValues.size() <= i) {
                        crawlContents(model,
                                startingShape,
                                trait,
                                shape.getMember(),
                                null,
                                element,
                                events,
                                path + '/' + i);
                    }
                }
            }
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            if (leftValue != null && rightValue != null && leftValue.isObjectNode() && rightValue.isObjectNode()) {
                Map<String, Node> leftValues = leftValue.expectObjectNode().getStringMap();
                Map<String, Node> rightValues = rightValue.expectObjectNode().getStringMap();

                // Look for changed and removed entries.
                for (Map.Entry<String, Node> entry : leftValues.entrySet()) {
                    Node rightValue = rightValues.get(entry.getKey());
                    crawlContents(model,
                            startingShape,
                            trait,
                            shape.getValue(),
                            entry.getValue(),
                            rightValue,
                            events,
                            path + '/' + entry.getKey());
                }

                // Look for added entries.
                for (Map.Entry<String, Node> entry : rightValues.entrySet()) {
                    if (!leftValues.containsKey(entry.getKey())) {
                        crawlContents(model,
                                startingShape,
                                trait,
                                shape.getValue(),
                                null,
                                entry.getValue(),
                                events,
                                path + '/' + entry.getKey());
                    }
                }
            }

            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            crawlStructuredShape(shape);
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            crawlStructuredShape(shape);
            return null;
        }

        private void crawlStructuredShape(Shape shape) {
            if (leftValue != null && rightValue != null && leftValue.isObjectNode() && rightValue.isObjectNode()) {
                ObjectNode leftObj = leftValue.expectObjectNode();
                ObjectNode rightObj = rightValue.expectObjectNode();
                for (MemberShape member : shape.members()) {
                    Node leftValue = leftObj.getMember(member.getMemberName()).orElse(null);
                    Node rightValue = rightObj.getMember(member.getMemberName()).orElse(null);
                    if (leftValue != null || rightValue != null) {
                        crawlContents(model,
                                startingShape,
                                trait,
                                member,
                                leftValue,
                                rightValue,
                                events,
                                path + '/' + member.getMemberName());
                    }
                }
            }
        }

        @Override
        public Void memberShape(MemberShape shape) {
            List<DiffStrategy> strategies = createStrategiesForShape(shape, false);
            for (DiffStrategy strategy : strategies) {
                events.addAll(strategy.diffType.validate(
                        model,
                        path,
                        startingShape,
                        trait,
                        leftValue,
                        rightValue,
                        strategy.severity));
            }

            // Recursively continue to crawl the shape and model.
            model.getShape(shape.getTarget()).ifPresent(target -> {
                crawlContents(model, startingShape, trait, target, leftValue, rightValue, events, path);
            });

            return null;
        }

        @Override
        protected Void getDefault(Shape shape) {
            return null;
        }
    }
}

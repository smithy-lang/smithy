/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.CycleException;
import software.amazon.smithy.utils.DependencyGraph;

final class LoaderShapeMap {
    private static final Logger LOGGER = Logger.getLogger(LoaderShapeMap.class.getName());

    private final Map<ShapeId, ShapeWrapper> shapes = new HashMap<>();
    private final Map<ShapeId, Shape> createdShapes = new HashMap<>();
    private final Model preludeShapes;
    private final List<ValidationEvent> events;

    LoaderShapeMap(Model prelude, List<ValidationEvent> events) {
        this.preludeShapes = prelude;
        this.events = events;
    }

    boolean isShapePending(ShapeId id) {
        // Check for root-level shapes first.
        if (containsShapeId(id)) {
            return true;
        }

        String member = id.getMember().orElse(null);
        if (member == null) {
            return false;
        }

        ShapeId root = id.withoutMember();
        return containsShapeId(root) && shapes.get(root).hasMember(member);
    }

    boolean isRootShapeDefined(ShapeId id) {
        return containsPreludeShape(id) || containsShapeId(id) || createdShapes.containsKey(id);
    }

    private boolean containsPreludeShape(ShapeId id) {
        return preludeShapes != null && preludeShapes.getShapeIds().contains(id);
    }

    private boolean containsShapeId(ShapeId id) {
        return shapes.containsKey(id);
    }

    ShapeType getShapeType(ShapeId id) {
        if (id.hasMember()) {
            // No need to descend into root shapes since members tell us their type in their shape ID.
            return ShapeType.MEMBER;
        } else if (shapes.containsKey(id)) {
            return shapes.get(id).getFirst().getShapeType();
        } else if (createdShapes.containsKey(id)) {
            return createdShapes.get(id).getType();
        } else if (containsPreludeShape(id)) {
            return preludeShapes.expectShape(id).getType();
        } else {
            return null;
        }
    }

    Version getShapeVersion(ShapeId shape) {
        ShapeId noMember = shape.withoutMember();
        if (shapes.containsKey(noMember)) {
            return shapes.get(noMember).getFirst().version;
        } else {
            return Version.UNKNOWN;
        }
    }

    ShapeWrapper get(ShapeId id) {
        ShapeWrapper result = shapes.get(id);
        if (result == null) {
            throw new IllegalArgumentException("Shape not found when loading the model: " + id);
        }
        return result;
    }

    void add(LoadOperation.DefineShape operation) {
        shapes.computeIfAbsent(operation.toShapeId(), id -> new ShapeWrapper()).add(operation);
    }

    void add(Shape shape, Consumer<LoadOperation> processor) {
        if (!shape.isMemberShape() && !Prelude.isPreludeShape(shape)) {
            createdShapes.put(shape.getId(), shape);
            // If the shape has mixins, then if the mixins are updated, we want those changes reflected in the shape.
            if (!shape.getMixins().isEmpty()) {
                moveCreatedShapeToOperations(shape.getId(), processor);
            }
        }
    }

    // If a shape was added as a created shape, but then something tries to modify it, then convert it to operations.
    void moveCreatedShapeToOperations(ShapeId shapeId, Consumer<LoadOperation> processor) {
        if (createdShapes.containsKey(shapeId)) {
            Shape shape = createdShapes.remove(shapeId);
            // Convert a created shape to a builder and add its members as builders.
            AbstractShapeBuilder<?, ?> builder = Shape.shapeToBuilder(shape);
            LoadOperation.DefineShape operation = new LoadOperation.DefineShape(Version.UNKNOWN, builder);
            // Remove and deconstruct mixins.
            for (ShapeId mixin : shape.getMixins()) {
                operation.addDependency(mixin);
                operation.addModifier(new ApplyMixin(mixin));
            }
            builder.clearMixins();
            // Remove traits from the shape and members and send them through the merging logic of loader
            // filtering out the synthetic ones to avoid attempting to reparse them.
            for (Trait trait : shape.getIntroducedTraits().values()) {
                // Special case round-tripping the box trait since it can be re-loaded and is needed in
                // order to round-trip models without causing equality issues.
                boolean notSynthetic = !trait.isSynthetic();
                if (notSynthetic || trait.toShapeId().equals(BoxTrait.ID)) {
                    processor.accept(LoadOperation.ApplyTrait.from(shape.getId(), trait));
                }
            }
            builder.clearTraits();
            // Clear out member mixins and traits, and register the newly created builders.
            for (MemberShape member : shape.members()) {
                MemberShape.Builder memberBuilder = member.toBuilder();
                for (Trait trait : member.getIntroducedTraits().values()) {
                    if (!trait.isSynthetic()) {
                        processor.accept(LoadOperation.ApplyTrait.from(member.getId(), trait));
                    }
                }
                memberBuilder.clearTraits().clearMixins();
                operation.addMember(memberBuilder);
            }
            add(operation);
        } else if (shapeId.hasMember()) {
            // If it was a member that was updated, then move it's root shape out of createdShapes.
            moveCreatedShapeToOperations(shapeId.withoutMember(), processor);
        }
    }

    void buildShapesAndClaimMixinTraits(
            Model.Builder modelBuilder,
            Function<ShapeId, Map<ShapeId, Trait>> unclaimedTraits
    ) {
        Function<ShapeId, Shape> createdShapeMap = id -> modelBuilder.getCurrentShapes().get(id);

        for (Shape shape : createdShapes.values()) {
            modelBuilder.addShapes(shape);
        }

        for (ShapeId id : sort()) {
            // If the shape isn't in `shapes` that means it isn't defined at all. ApplyMixins will
            // emit a specific error regarding the missing mixin.
            if (!createdShapes.containsKey(id) && shapes.containsKey(id)) {
                buildIntoModel(shapes.get(id), modelBuilder, unclaimedTraits, createdShapeMap);
            }
        }
    }

    // Build each pending shape in the wrapper and perform conflict resolution.
    private void buildIntoModel(
            ShapeWrapper wrapper,
            Model.Builder builder,
            Function<ShapeId, Map<ShapeId, Trait>> unclaimedTraits,
            Function<ShapeId, Shape> createdShapeMap
    ) {
        Shape built = null;
        for (LoadOperation.DefineShape shape : wrapper) {
            if (validateShapeVersion(shape)) {
                Shape newShape = buildShape(shape, unclaimedTraits, createdShapeMap);
                if (newShape != null) {
                    if (validateConflicts(shape.toShapeId(), newShape, built)) {
                        built = newShape;
                    }
                }
            }
        }
        if (built != null) {
            builder.addShape(built);
        }
    }

    private List<ShapeId> sort() {
        DependencyGraph<ShapeId> dependencyGraph = new DependencyGraph<>();
        for (Shape shape : createdShapes.values()) {
            dependencyGraph.add(shape.toShapeId());
            dependencyGraph.addDependencies(shape.toShapeId(), shape.getMixins());
        }

        for (Map.Entry<ShapeId, ShapeWrapper> entry : shapes.entrySet()) {
            dependencyGraph.add(entry.getKey());
            dependencyGraph.setDependencies(entry.getKey(), entry.getValue().dependencies());
        }

        try {
            return dependencyGraph.toSortedList();
        } catch (CycleException e) {
            // We know there is at least one cycle, now we use findCycles to
            // identify each individual cycle so that we can give better error
            // messages.
            List<List<ShapeId>> cycles = dependencyGraph.findCycles();
            for (List<ShapeId> cycle : cycles) {
                for (ShapeId participant : cycle) {
                    // Remove the shape that the event is attached to from the list that
                    // will be added to the message for brevity.
                    List<ShapeId> filtered = new ArrayList<>(cycle);
                    filtered.remove(participant);
                    for (LoadOperation.DefineShape shape : get(participant)) {
                        events.add(ValidationEvent.builder()
                                .id(Validator.MODEL_ERROR)
                                .severity(Severity.ERROR)
                                .shapeId(participant)
                                .sourceLocation(shape)
                                .message("Unable to resolve mixins; cycles detected between this shape and " + filtered)
                                .build());
                    }
                }
            }
            return e.getSortedNodes(ShapeId.class);
        }
    }

    private boolean validateShapeVersion(LoadOperation.DefineShape operation) {
        if (!operation.version.isShapeTypeSupported(operation.getShapeType())) {
            events.add(ValidationEvent.builder()
                    .severity(Severity.ERROR)
                    .id(Validator.MODEL_ERROR)
                    .shapeId(operation.toShapeId())
                    .sourceLocation(operation)
                    .message(String.format(
                            "%s shapes cannot be used in Smithy version " + operation.version,
                            operation.getShapeType()))
                    .build());
            return false;
        }
        return true;
    }

    private boolean validateConflicts(ShapeId id, Shape built, Shape previous) {
        if (previous != null && built != null) {
            if (!previous.equals(built)) {
                // Create a small diff to make it easier to diagnose conflicts.
                StringJoiner joiner = new StringJoiner("; ");
                if (built.getType() != previous.getType()) {
                    joiner.add("Left is " + built.getType() + ", right is " + previous.getType());
                }
                if (!built.getMixins().equals(previous.getMixins())) {
                    joiner.add("Left mixins: " + built.getMixins() + ", right mixins: " + previous.getMixins());
                }
                if (!built.getAllTraits().equals(previous.getAllTraits())) {
                    built.getAllTraits().forEach((tid, t) -> {
                        if (!previous.hasTrait(tid)) {
                            joiner.add("Left has trait " + tid);
                        } else if (!previous.getAllTraits().get(tid).equals(t)) {
                            joiner.add("Left trait " + tid + " differs from right trait. "
                                    + Node.printJson(t.toNode()) + " vs "
                                    + Node.printJson(previous.getAllTraits().get(tid).toNode()));
                        }
                    });
                    previous.getAllTraits().forEach((tid, t) -> {
                        if (!built.hasTrait(tid)) {
                            joiner.add("Right has trait " + tid);
                        }
                    });
                }
                if (!built.getAllMembers().equals(previous.getAllMembers())) {
                    joiner.add("Members differ: " + built.getAllMembers().keySet()
                            + " vs " + previous.getAllMembers().keySet());
                }
                events.add(LoaderUtils.onShapeConflict(id,
                        built.getSourceLocation(),
                        previous.getSourceLocation(),
                        joiner.toString()));
                return false;
            } else if (!LoaderUtils.isSameLocation(built, previous)) {
                events.add(ValidationEvent.builder()
                        .id(Validator.MODEL_ERROR + ".IgnoredDuplicateDefinition")
                        .severity(Severity.NOTE)
                        .sourceLocation(previous.getSourceLocation())
                        .shapeId(id)
                        .message("Ignoring duplicate but equivalent shape definition: " + id
                                + " defined at " + built.getSourceLocation() + " and "
                                + previous.getSourceLocation())
                        .build());
            }
        }
        return true;
    }

    private Shape buildShape(
            LoadOperation.DefineShape defineShape,
            Function<ShapeId, Map<ShapeId, Trait>> traitClaimer,
            Function<ShapeId, Shape> createdShapeMap
    ) {
        try {
            AbstractShapeBuilder<?, ?> builder = defineShape.builder();
            ModelInteropTransformer.patchShapeBeforeBuilding(defineShape, builder, events);

            for (MemberShape.Builder memberBuilder : defineShape.memberBuilders().values()) {
                for (ShapeModifier modifier : defineShape.modifiers()) {
                    modifier.modifyMember(builder, memberBuilder, traitClaimer, createdShapeMap);
                }
                MemberShape member = buildMember(memberBuilder);
                if (member != null) {
                    // Adding a member may throw, but we want to continue execution, so we collect all
                    // errors that occur.
                    try {
                        builder.addMember(member);
                    } catch (SourceException e) {
                        events.add(ValidationEvent.fromSourceException(e, "", builder.getId()));
                    }
                }
            }

            for (ShapeModifier modifier : defineShape.modifiers()) {
                modifier.modifyShape(builder, defineShape.memberBuilders(), traitClaimer, createdShapeMap);
                events.addAll(modifier.getEvents());
            }

            return builder.build();
        } catch (SourceException e) {
            events.add(ValidationEvent.fromSourceException(e, "", defineShape.toShapeId()));
            return null;
        }
    }

    private MemberShape buildMember(MemberShape.Builder builder) {
        try {
            return builder.build();
        } catch (IllegalStateException e) {
            if (builder.getTarget() == null) {
                events.add(ValidationEvent.builder()
                        .severity(Severity.ERROR)
                        .id(Validator.MODEL_ERROR)
                        .shapeId(builder.getId())
                        .sourceLocation(builder)
                        .message("Member target was elided, but no bound resource or mixin contained a matching "
                                + "identifier or member name.")
                        .build());
                return null;
            }
            throw e;
        } catch (SourceException e) {
            events.add(ValidationEvent.fromSourceException(e, "", builder.getId()));
            return null;
        }
    }

    // Aggregates shapes with the same ID before LoaderShapeMap later de-conflicts them as they're built.
    static final class ShapeWrapper implements Iterable<LoadOperation.DefineShape> {
        private final List<LoadOperation.DefineShape> shapes = new ArrayList<>(1);

        @Override
        public Iterator<LoadOperation.DefineShape> iterator() {
            return shapes.iterator();
        }

        LoadOperation.DefineShape getFirst() {
            return shapes.get(0);
        }

        void add(LoadOperation.DefineShape shape) {
            shapes.add(shape);
        }

        boolean hasMember(String memberName) {
            for (LoadOperation.DefineShape shape : this) {
                if (shape.hasMember(memberName)) {
                    return true;
                }
            }
            return false;
        }

        Set<ShapeId> dependencies() {
            // Dependencies have to be computed each time because the deps on a shape can change.
            if (shapes.size() == 1) {
                return getFirst().dependencies();
            } else if (!hasDependencies()) {
                return Collections.emptySet();
            } else {
                Set<ShapeId> dependencies = new HashSet<>();
                for (LoadOperation.DefineShape shape : shapes) {
                    dependencies.addAll(shape.dependencies());
                }
                return dependencies;
            }
        }

        private boolean hasDependencies() {
            for (LoadOperation.DefineShape shape : shapes) {
                if (!shape.dependencies().isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }
}

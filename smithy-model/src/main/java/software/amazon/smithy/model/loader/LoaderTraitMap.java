/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static java.lang.String.format;
import static software.amazon.smithy.model.validation.Severity.ERROR;
import static software.amazon.smithy.model.validation.Validator.MODEL_ERROR;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

final class LoaderTraitMap {

    private static final Logger LOGGER = Logger.getLogger(LoaderTraitMap.class.getName());
    private static final String UNRESOLVED_TRAIT_SUFFIX = ".UnresolvedTrait";

    private final TraitFactory traitFactory;
    private final Map<ShapeId, Map<ShapeId, Node>> traits = new HashMap<>();
    private final List<ValidationEvent> events;
    private final boolean allowUnknownTraits;
    private final Map<ShapeId, Map<ShapeId, Trait>> unclaimed = new HashMap<>();
    private final Set<ShapeId> claimed = new HashSet<>();

    LoaderTraitMap(TraitFactory traitFactory, List<ValidationEvent> events, boolean allowUnknownTraits) {
        this.traitFactory = traitFactory;
        this.events = events;
        this.allowUnknownTraits = allowUnknownTraits;
    }

    void applyTraitsToNonMixinsInShapeMap(LoaderShapeMap shapeMap) {
        for (Map.Entry<ShapeId, Map<ShapeId, Node>> entry : traits.entrySet()) {
            ShapeId target = entry.getKey();
            ShapeId root = target.withoutMember();

            // Check if the actual shape (or member) is found, but grab the member-less shape from the shape map.
            // Only pending shapes are checked here. If a trait was added to a built shape, then LoadOperationProcessor
            // will have already converted the built shape into a pending shape.
            boolean found = shapeMap.isShapePending(target);
            Iterable<LoadOperation.DefineShape> rootShapes = found
                    ? shapeMap.get(root)
                    : Collections::emptyIterator;

            for (Map.Entry<ShapeId, Node> traitEntry : entry.getValue().entrySet()) {
                ShapeId traitId = traitEntry.getKey();
                Node traitNode = traitEntry.getValue();
                Trait created = createTrait(target, traitId, traitNode);
                validateTraitIsKnown(target, traitId, created, traitNode.getSourceLocation(), shapeMap);

                if (target.hasMember()) {
                    // Apply the trait to a member by reaching into the members of each LoadOperation.DefineShape.
                    String memberName = target.getMember().get();
                    boolean foundMember = false;
                    for (LoadOperation.DefineShape shape : rootShapes) {
                        if (shape.hasMember(memberName)) {
                            foundMember = true;
                            applyTraitsToShape(shape.memberBuilders().get(memberName), created);
                        } else {
                            // If we didn't have the member and the member is from a mixin,
                            // we need to update ApplyMixin shape modifiers to apply the trait
                            // in case we have the target's container already in the shapeMap.
                            for (ShapeModifier modifier : shape.modifiers()) {
                                if (modifier instanceof ApplyMixin) {
                                    ((ApplyMixin) modifier).putPotentiallyIntroducedTrait(target, created);
                                }
                            }
                        }
                    }

                    // If the member wasn't found, then it might be a mixin member that is synthesized later.
                    if (!foundMember) {
                        unclaimed.computeIfAbsent(target.withMember(memberName), id -> new LinkedHashMap<>())
                                .put(traitId, created);
                    }
                } else if (found) {
                    // Apply the trait to each shape contained in the shape map for the given target.
                    for (LoadOperation.DefineShape shape : rootShapes) {
                        applyTraitsToShape(shape.builder(), created);
                    }
                } else {
                    unclaimed.computeIfAbsent(target, id -> new LinkedHashMap<>()).put(traitId, created);
                }
            }
        }
    }

    private Trait createTrait(ShapeId target, ShapeId traitId, Node traitValue) {
        try {
            return traitFactory.createTrait(traitId, target, traitValue)
                    .orElseGet(() -> new DynamicTrait(traitId, traitValue));
        } catch (SourceException e) {
            String message = format("Error creating trait `%s`: ", Trait.getIdiomaticTraitName(traitId));
            events.add(ValidationEvent.fromSourceException(e, message, target));
            return null;
        } catch (RuntimeException e) {
            events.add(ValidationEvent.builder()
                    .id(MODEL_ERROR)
                    .severity(ERROR)
                    .shapeId(target)
                    .sourceLocation(traitValue)
                    .message(format("Error creating trait `%s`: %s",
                            Trait.getIdiomaticTraitName(traitId),
                            e.getMessage()))
                    .build());
            return null;
        }
    }

    private void validateTraitIsKnown(
            ShapeId target,
            ShapeId traitId,
            Trait trait,
            SourceLocation sourceLocation,
            LoaderShapeMap shapeMap
    ) {
        if (!shapeMap.isRootShapeDefined(traitId) && (trait == null || !trait.isSynthetic())) {
            Severity severity = allowUnknownTraits ? Severity.WARNING : Severity.ERROR;
            events.add(ValidationEvent.builder()
                    .id(Validator.MODEL_ERROR + UNRESOLVED_TRAIT_SUFFIX)
                    .severity(severity)
                    .sourceLocation(sourceLocation)
                    .shapeId(target)
                    .message(String.format("Unable to resolve trait `%s`. If this is a custom trait, then it must be "
                            + "defined before it can be used in a model.", traitId))
                    .build());
        }
    }

    private void applyTraitsToShape(AbstractShapeBuilder<?, ?> shape, Trait trait) {
        if (trait != null) {
            shape.addTrait(trait);
        }
    }

    // Traits can be applied to synthesized members inherited from mixins. Applying these traits is deferred until
    // the point in which mixin members are synthesized into shapes.
    Map<ShapeId, Trait> claimTraitsForShape(ShapeId id) {
        if (!unclaimed.containsKey(id)) {
            return Collections.emptyMap();
        }
        claimed.add(id);
        return unclaimed.get(id);
    }

    // Emit events if any traits were applied to shapes that weren't found in the model.
    void emitUnclaimedTraits() {
        for (Map.Entry<ShapeId, Map<ShapeId, Trait>> entry : unclaimed.entrySet()) {
            if (claimed.contains(entry.getKey())) {
                continue;
            }
            for (Map.Entry<ShapeId, Trait> traitEntry : entry.getValue().entrySet()) {
                events.add(ValidationEvent.builder()
                        .id(Validator.MODEL_ERROR)
                        .severity(Severity.ERROR)
                        .sourceLocation(traitEntry.getValue())
                        .message(String.format("Trait `%s` applied to unknown shape `%s`",
                                Trait.getIdiomaticTraitName(traitEntry.getKey()),
                                entry.getKey()))
                        .build());
            }
        }
    }

    void add(LoadOperation.ApplyTrait operation) {
        if (validateTraitVersion(operation)) {
            if (isAppliedToPreludeOutsidePrelude(operation)) {
                String message = String.format(
                        "Cannot apply `%s` to an immutable prelude shape defined in `smithy.api`.",
                        operation.trait);
                events.add(ValidationEvent.builder()
                        .severity(Severity.ERROR)
                        .id(Validator.MODEL_ERROR)
                        .sourceLocation(operation)
                        .shapeId(operation.target)
                        .message(message)
                        .build());
            } else {
                Map<ShapeId, Node> current = traits.computeIfAbsent(operation.target, id -> new LinkedHashMap<>());
                Node previous = current.get(operation.trait);
                current.put(operation.trait, mergeTraits(operation.target, operation.trait, previous, operation.value));
            }
        }
    }

    private boolean validateTraitVersion(LoadOperation.ApplyTrait operation) {
        ValidationEvent event = operation.version.validateVersionedTrait(
                operation.target,
                operation.trait,
                operation.value);
        if (event != null) {
            events.add(event);
        }
        return true;
    }

    private boolean isAppliedToPreludeOutsidePrelude(LoadOperation.ApplyTrait operation) {
        return !operation.namespace.equals(Prelude.NAMESPACE)
                && operation.target.getNamespace().equals(Prelude.NAMESPACE);
    }

    private Node mergeTraits(ShapeId target, ShapeId traitId, Node previous, Node updated) {
        if (previous == null) {
            return updated;
        }

        if (LoaderUtils.isSameLocation(previous, updated) && previous.equals(updated)) {
            // The assumption here is that if the trait value is exactly the
            // same and from the same location, then the same model file was
            // included more than once in a way that side-steps file and URL
            // de-duplication. For example, this can occur when a Model is assembled
            // through a ModelAssembler using model discovery, then the Model is
            // added to a subsequent ModelAssembler, and then model discovery is
            // performed again using the same classpath.
            LOGGER.finest(() -> String.format("Ignoring duplicate %s trait value on %s at same exact location",
                    traitId,
                    target));
            return previous;
        }

        if (previous.isArrayNode() && updated.isArrayNode()) {
            // You can merge trait arrays.
            return previous.expectArrayNode().merge(updated.expectArrayNode());
        } else if (previous.equals(updated)) {
            LOGGER.fine(() -> String.format("Ignoring duplicate %s trait value on %s", traitId, target));
            return previous;
        } else {
            events.add(ValidationEvent.builder()
                    .id(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .sourceLocation(updated)
                    .shapeId(target)
                    .message(String.format("Conflicting `%s` trait found on shape `%s`. The previous trait was "
                            + "defined at `%s`, and a conflicting trait was defined at `%s`.",
                            traitId,
                            target,
                            previous.getSourceLocation(),
                            updated.getSourceLocation()))
                    .build());
            return previous;
        }
    }
}

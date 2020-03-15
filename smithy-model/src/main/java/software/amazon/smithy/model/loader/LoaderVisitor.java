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

package software.amazon.smithy.model.loader;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Visitor used to drive the creation of a Model.
 *
 * <p>The intent of this visitor is to decouple the serialized format of a
 * Smithy model from the in-memory Model representation. This allows for
 * deserialization code to focus on just extracting data from the model
 * rather than logic around duplication detection, trait loading, etc.
 */
final class LoaderVisitor {
    private static final Logger LOGGER = Logger.getLogger(LoaderVisitor.class.getName());
    private static final TraitDefinition.Provider TRAIT_DEF_PROVIDER = new TraitDefinition.Provider();

    /** Factory used to create traits. */
    private final TraitFactory traitFactory;

    /** Property bag used to configure the LoaderVisitor. */
    private final Map<String, Object> properties;

    /** Validation events encountered while loading the model. */
    private final List<ValidationEvent> events = new ArrayList<>();

    /** Model metadata to assemble together. */
    private final Map<String, Node> metadata = new HashMap<>();

    /** Map of shape IDs to a list of traits to apply to the shape once they're built. */
    private final Map<ShapeId, List<PendingTrait>> pendingTraits = new HashMap<>();

    /** References that need to be resolved against a namespace or the prelude. */
    private final List<ForwardReferenceResolver> forwardReferenceResolvers = new ArrayList<>();

    /** Shapes that have yet to be built. */
    private final Map<ShapeId, AbstractShapeBuilder> pendingShapes = new HashMap<>();

    /** Built shapes to add to the Model. Keys are not allowed to conflict with pendingShapes. */
    private final Map<ShapeId, Shape> builtShapes = new HashMap<>();

    /** Built trait definitions. */
    private final Map<ShapeId, TraitDefinition> builtTraitDefinitions = new HashMap<>();

    /** The result that is populated when onEnd is called. */
    private ValidatedResult<Model> result;

    private static final class PendingTrait {
        final ShapeId id;
        final Node value;
        final Trait trait;

        // A pending trait that needs to be created.
        PendingTrait(ShapeId id, Node value) {
            this.id = id;
            this.value = value;
            this.trait = null;
        }

        // A pending trait that's already created.
        PendingTrait(ShapeId id, Trait trait) {
            this.id = id;
            this.trait = trait;
            this.value = null;
        }
    }

    private static final class ForwardReferenceResolver {
        final ShapeId expectedId;
        final Consumer<ShapeId> consumer;

        ForwardReferenceResolver(ShapeId expectedId, Consumer<ShapeId> consumer) {
            this.expectedId = expectedId;
            this.consumer = consumer;
        }
    }

    /**
     * @param traitFactory Trait factory to use when resolving traits.
     */
    LoaderVisitor(TraitFactory traitFactory) {
        this(traitFactory, Collections.emptyMap());
    }

    /**
     * @param traitFactory Trait factory to use when resolving traits.
     * @param properties Map of loader visitor properties.
     */
    LoaderVisitor(TraitFactory traitFactory, Map<String, Object> properties) {
        this.traitFactory = traitFactory;
        this.properties = properties;
    }

    /**
     * Checks if the LoaderVisitor has defined a specific shape.
     *
     * @param id Shape ID to check.
     * @return Returns true if the shape has been defined.
     */
    public boolean hasDefinedShape(ShapeId id) {
        return builtShapes.containsKey(id) || pendingShapes.containsKey(id);
    }

    /**
     * Checks if a specific property is set.
     *
     * @param property Name of the property to check.
     * @return Returns true if the property is set.
     */
    public boolean hasProperty(String property) {
        return properties.containsKey(property);
    }

    /**
     * Gets a property from the loader visitor.
     *
     * @param property Name of the property to get.
     * @return Returns the optionally found property.
     */
    public Optional<Object> getProperty(String property) {
        return Optional.ofNullable(properties.get(property));
    }

    /**
     * Gets a property from the loader visitor of a specific type.
     *
     * @param property Name of the property to get.
     * @param <T> Type to convert the property to if found.
     * @param type Type to convert the property to if found.
     * @return Returns the optionally found property.
     * @throws ClassCastException if a found property is not of the expected type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String property, Class<T> type) {
        return getProperty(property).map(value -> {
            if (!type.isInstance(value)) {
                throw new ClassCastException(String.format(
                        "Expected `%s` property of the LoaderVisitor to be a `%s`, but found a `%s`",
                        property, type.getName(), value.getClass().getName()));
            }
            return (T) value;
        });
    }

    /**
     * Adds an error to the loader.
     *
     * @param event Validation event to add.
     */
    public void onError(ValidationEvent event) {
        events.add(Objects.requireNonNull(event));
    }

    /**
     * Adds a shape to the loader.
     *
     * @param shapeBuilder Shape builder to add.
     */
    public void onShape(AbstractShapeBuilder shapeBuilder) {
        ShapeId id = SmithyBuilder.requiredState("id", shapeBuilder.getId());
        if (validateOnShape(id, shapeBuilder)) {
            pendingShapes.put(id, shapeBuilder);
        }
    }

    /**
     * Adds a shape to the loader.
     *
     * @param shape Built shape to add to the loader visitor.
     */
    public void onShape(Shape shape) {
        if (validateOnShape(shape.getId(), shape)) {
            builtShapes.put(shape.getId(), shape);
        }

        // Whether or not a shape defines a trait needs to be tracked when
        // adding already built shapes (e.g., this happens with the prelude model).
        shape.getTrait(TraitDefinition.class).ifPresent(def -> onTraitDefinition(shape.getId(), def));
    }

    private void onTraitDefinition(ShapeId target, TraitDefinition definition) {
        builtTraitDefinitions.put(target, definition);
    }

    private boolean validateOnShape(ShapeId id, FromSourceLocation source) {
        if (!hasDefinedShape(id)) {
            return true;
        } else if (Prelude.isPreludeShape(id)) {
            // Ignore prelude shape conflicts since it's such a common case of
            // passing an already built model into a ModelAssembler.
        } else {
            // The shape has been duplicated, so get the previously defined pending shape or built shape.
            SourceLocation previous = Optional.<FromSourceLocation>ofNullable(pendingShapes.get(id))
                    .orElseGet(() -> builtShapes.get(id)).getSourceLocation();
            // Cannot ignore duplicate member definitions.
            boolean canIgnore = !id.getMember().isPresent()
                                // Ignore duplicate shapes defined in the same file.
                                && previous != SourceLocation.NONE
                                && previous.equals(source.getSourceLocation());
            if (canIgnore) {
                LOGGER.warning(() -> "Ignoring duplicate shape definition defined in the same file: "
                                     + id + " defined at " + source.getSourceLocation());
            } else {
                onError(ValidationEvent.builder()
                                .shapeId(id)
                                .eventId(Validator.MODEL_ERROR)
                                .severity(Severity.ERROR)
                                .sourceLocation(source)
                                .message(String.format(
                                        "Duplicate shape definition for `%s` found at `%s` and `%s`",
                                        id, previous.getSourceLocation(), source.getSourceLocation()))
                                .build());
            }
        }

        return false;
    }

    /**
     * Adds a trait to a shape.
     *
     * <p>Resolving the trait against a trait definition is deferred until
     * the entire model is loaded. A namespace is required to have been set
     * if the trait name is not absolute.
     *
     * @param target Shape to add the trait to.
     * @param trait SHape ID of the trait to add.
     * @param traitValue Trait value as a Node object.
     */
    public void onTrait(ShapeId target, ShapeId trait, Node traitValue) {
        // Special handling for the loading of trait definitions. These need to be
        // loaded first before other traits can be resolved.
        if (trait.equals(TraitDefinition.ID)) {
            TraitDefinition traitDef = TRAIT_DEF_PROVIDER.createTrait(target, traitValue);
            // Register this as a trait definition to resolve against pending traits.
            onTraitDefinition(target, traitDef);
            // Add the definition trait to the shape.
            onTrait(target, traitDef);
        } else {
            PendingTrait pendingTrait = new PendingTrait(trait, traitValue);
            addPendingTrait(target, traitValue.getSourceLocation(), trait, pendingTrait);
        }
    }

    /**
     * Adds a resolved and parsed trait to a shape.
     *
     * @param target Shape to add the trait to.
     * @param trait Trait to add to the shape.
     */
    public void onTrait(ShapeId target, Trait trait) {
        PendingTrait pending = new PendingTrait(target, trait);
        addPendingTrait(target, trait.getSourceLocation(), trait.toShapeId(), pending);
    }

    private void addPendingTrait(ShapeId target, SourceLocation sourceLocation, ShapeId trait, PendingTrait pending) {
        if (Prelude.isImmutablePublicPreludeShape(target)) {
            onError(ValidationEvent.builder()
                    .severity(Severity.ERROR)
                    .eventId(Validator.MODEL_ERROR)
                    .sourceLocation(sourceLocation)
                    .shapeId(target)
                    .message(String.format(
                            "Cannot apply `%s` to an immutable prelude shape defined in `smithy.api`.", trait))
                    .build());
        } else {
            pendingTraits.computeIfAbsent(target, targetId -> new ArrayList<>()).add(pending);
        }
    }

    /**
     * Adds a forward reference that is resolved once all shapes have been loaded.
     *
     * @param expectedId The shape ID that would be resolved in the current namespace.
     * @param consumer The consumer that receives the resolved shape ID.
     */
    void addForwardReference(ShapeId expectedId, Consumer<ShapeId> consumer) {
        forwardReferenceResolvers.add(new ForwardReferenceResolver(expectedId, consumer));
    }

    /**
     * Adds metadata to the loader.
     *
     * @param key Metadata key to add.
     * @param value Metadata value to add.
     */
    public void onMetadata(String key, Node value) {
        if (!metadata.containsKey(key)) {
            metadata.put(key, value);
        } else if (metadata.get(key).isArrayNode() && value.isArrayNode()) {
            ArrayNode previous = metadata.get(key).expectArrayNode();
            List<Node> merged = new ArrayList<>(previous.getElements());
            merged.addAll(value.expectArrayNode().getElements());
            ArrayNode mergedArray = new ArrayNode(merged, value.getSourceLocation());
            metadata.put(key, mergedArray);
        } else if (!metadata.get(key).equals(value)) {
            onError(ValidationEvent.builder()
                    .eventId(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .sourceLocation(value)
                    .message(format(
                            "Metadata conflict for key `%s`. Defined in both `%s` and `%s`",
                            key, value.getSourceLocation(), metadata.get(key).getSourceLocation()))
                    .build());
        } else {
            LOGGER.fine(() -> "Ignoring duplicate metadata definition of " + key);
        }
    }

    /**
     * Called when the visitor has completed.
     *
     * @return Returns the validated model result.
     */
    public ValidatedResult<Model> onEnd() {
        if (result != null) {
            return result;
        }

        Model.Builder modelBuilder = Model.builder().metadata(metadata);

        finalizeShapeTargets();
        finalizePendingTraits();

        // Ensure that shape builders are created for the container shapes of
        // each modified member (builders were already created if a shape has
        // a pending trait). This needs to be done before iterating over the
        // pending shapes because a collection can't be modified while
        // iterating.
        List<ShapeId> needsConversion = new ArrayList<>();
        for (AbstractShapeBuilder builder : pendingShapes.values()) {
            if (builder instanceof MemberShape.Builder) {
                needsConversion.add(builder.getId().withoutMember());
            }
        }
        needsConversion.forEach(this::resolveShapeBuilder);

        // Build members and add them to their containing shape builders.
        for (AbstractShapeBuilder shape : pendingShapes.values()) {
            if (shape.getClass() == MemberShape.Builder.class) {
                MemberShape member = (MemberShape) buildShape(modelBuilder, shape);
                if (member != null) {
                    AbstractShapeBuilder container = pendingShapes.get(shape.getId().withoutMember());
                    if (container == null) {
                        events.add(ValidationEvent.builder()
                                .shape(member)
                                .eventId(Validator.MODEL_ERROR)
                                .severity(Severity.ERROR)
                                .message(format("Member shape `%s` added to non-existent shape", member.getId()))
                                .build());
                    } else {
                        container.addMember(member);
                    }
                }
            }
        }

        // Now that members were built, build all non-members.
        for (AbstractShapeBuilder shape : pendingShapes.values()) {
            if (shape.getClass() != MemberShape.Builder.class) {
                buildShape(modelBuilder, shape);
            }
        }

        // Add any remaining built shapes.
        modelBuilder.addShapes(builtShapes.values());
        result = new ValidatedResult<>(modelBuilder.build(), events);

        return result;
    }

    private void finalizeShapeTargets() {
        // Run any finalizers used for things like forward reference resolution.
        for (ForwardReferenceResolver resolver : forwardReferenceResolvers) {
            // First, resolve to a shape in the current namespace if one exists.
            if (!hasDefinedShape(resolver.expectedId)) {
                // Next resolve to a prelude shape if one exists and is public.
                ShapeId preludeId = ShapeId.fromParts(Prelude.NAMESPACE, resolver.expectedId.asRelativeReference());
                if (Prelude.isPublicPreludeShape(preludeId)) {
                    resolver.consumer.accept(preludeId);
                    continue;
                }
                // Finally, just default back to original namespace using a broken target.
            }
            resolver.consumer.accept(resolver.expectedId);
        }

        forwardReferenceResolvers.clear();
    }

    private void finalizePendingTraits() {
        // Build trait nodes and add them to their shape builders.
        for (Map.Entry<ShapeId, List<PendingTrait>> entry : pendingTraits.entrySet()) {
            ShapeId target = entry.getKey();
            List<PendingTrait> pendingTraits = entry.getValue();
            AbstractShapeBuilder builder = resolveShapeBuilder(target);
            if (builder == null) {
                // The shape was not found, so emit a validation event for every trait applied to it.
                emitErrorsForEachInvalidTraitTarget(target, pendingTraits);
                continue;
            }

            // Add already built traits to the shape. Note that these kinds of
            // traits *could* be overwritten by traits defined in the model.
            // However, that is only technically possible with the documentation
            // trait since one is manually created for documentation comments.
            for (PendingTrait pending : pendingTraits) {
                if (pending.trait != null) {
                    builder.addTrait(pending.trait);
                }
            }

            // Compute the shapes to add and merge into the shape.
            for (Map.Entry<ShapeId, Node> computedEntry : computeTraits(builder, pendingTraits).entrySet()) {
                createAndApplyTraitToShape(builder, computedEntry.getKey(), computedEntry.getValue());
            }
        }
    }

    private AbstractShapeBuilder resolveShapeBuilder(ShapeId id) {
        if (pendingShapes.containsKey(id)) {
            return pendingShapes.get(id);
        } else if (builtShapes.containsKey(id)) {
            // If the shape is not a builder but rather a built shape, then convert into a builder.
            // Once converted, the shape is removed from builtShapes and added into pendingShapes.
            AbstractShapeBuilder builder = (AbstractShapeBuilder) Shape.shapeToBuilder(builtShapes.remove(id));
            pendingShapes.put(id, builder);
            return builder;
        } else {
            return null;
        }
    }

    private void emitErrorsForEachInvalidTraitTarget(ShapeId target, List<PendingTrait> pendingTraits) {
        for (PendingTrait pendingTrait : pendingTraits) {
            onError(ValidationEvent.builder()
                    .eventId(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .sourceLocation(pendingTrait.value.getSourceLocation())
                    .message(format("Trait `%s` applied to unknown shape `%s`",
                                    Trait.getIdiomaticTraitName(pendingTrait.id), target))
                    .build());
        }
    }

    private Shape buildShape(Model.Builder modelBuilder, AbstractShapeBuilder shapeBuilder) {
        try {
            Shape result = (Shape) shapeBuilder.build();
            modelBuilder.addShape(result);
            return result;
        } catch (SourceException e) {
            onError(ValidationEvent.fromSourceException(e).toBuilder().shapeId(shapeBuilder.getId()).build());
            return null;
        }
    }

    /**
     * This method resolves the fully-qualified names of each pending trait
     * (checking the namespace that contains the trait and the prelude), merges
     * them if necessary, and returns the merged map of trait names to trait
     * node values.
     *
     * Traits are added to a flat list of pending traits for a shape. We can
     * only actually determine the resolved trait definition to apply once the
     * entire model is loaded an all trait definitions are known. As such,
     * the logic for merging traits and detecting duplicates is deferred until
     * the end of the model is detected.
     *
     * @param shapeBuilder Shape that is being resolved.
     * @param pending The list of pending traits to resolve.
     * @return Returns the resolved map of pending traits.
     */
    private Map<ShapeId, Node> computeTraits(AbstractShapeBuilder shapeBuilder, List<PendingTrait> pending) {
        Map<ShapeId, Node> traits = new HashMap<>();
        for (PendingTrait trait : pending) {
            // Already resolved traits don't need to be computed.
            if (trait.trait != null) {
                continue;
            }

            TraitDefinition definition = builtTraitDefinitions.get(trait.id);

            if (definition == null) {
                onUnresolvedTraitName(shapeBuilder, trait);
                continue;
            }

            ShapeId traitId = trait.id;
            Node value = coerceTraitValue(trait.value, trait.id);
            Node previous = traits.get(traitId);

            if (previous == null) {
                traits.put(traitId, value);
            } else if (previous.isArrayNode() && value.isArrayNode()) {
                // You can merge trait arrays.
                traits.put(traitId, value.asArrayNode().get().merge(previous.asArrayNode().get()));
            } else if (previous.equals(value)) {
                LOGGER.fine(() -> String.format(
                        "Ignoring duplicate %s trait value on %s", traitId, shapeBuilder.getId()));
            } else {
                onDuplicateTrait(shapeBuilder.getId(), traitId, previous, value);
            }
        }

        return traits;
    }

    private Node coerceTraitValue(Node value, ShapeId traitId) {
        return Trait.coerceTraitValue(value, determineTraitDefinitionType(traitId));
    }

    private ShapeType determineTraitDefinitionType(ShapeId traitId) {
        assert pendingShapes.containsKey(traitId) || builtShapes.containsKey(traitId);

        if (pendingShapes.containsKey(traitId)) {
            return pendingShapes.get(traitId).getShapeType();
        } else {
            return builtShapes.get(traitId).getType();
        }
    }

    private void onDuplicateTrait(ShapeId target, ShapeId traitName, FromSourceLocation previous, Node duplicate) {
        onError(ValidationEvent.builder()
                .eventId(Validator.MODEL_ERROR)
                .severity(Severity.ERROR)
                .sourceLocation(duplicate.getSourceLocation())
                .shapeId(target)
                .message(format(
                        "Conflicting `%s` trait found on shape `%s`. The previous trait was defined at `%s`, "
                        + "and a conflicting trait was defined at `%s`.",
                        traitName, target, previous.getSourceLocation(), duplicate.getSourceLocation()))
                .build());
    }

    private void onUnresolvedTraitName(AbstractShapeBuilder shapeBuilder, PendingTrait trait) {
        Severity severity = getProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, Boolean.class).orElse(false)
                            ? Severity.WARNING : Severity.ERROR;

        // Fail if the trait cannot be resolved.
        onError(ValidationEvent.builder()
                .eventId(Validator.MODEL_ERROR)
                .severity(severity)
                .sourceLocation(trait.value.getSourceLocation())
                .shapeId(shapeBuilder.getId())
                .message(format("Unable to resolve trait `%s`. If this is a custom trait, then it must be "
                                + "defined before it can be used in a model.", trait.id))
                .build());
    }

    /**
     * Applies a trait to a shape. If a concrete class for the trait cannot
     * be found, then a {@link DynamicTrait} is created. If the trait throws
     * an exception while being created, it is caught and the validation
     * event is logged.
     *
     * @param shapeBuilder Shape builder to update.
     * @param traitId ID of the fully-qualified trait to add.
     * @param traitValue The trait value to set.
     */
    private void createAndApplyTraitToShape(AbstractShapeBuilder shapeBuilder, ShapeId traitId, Node traitValue) {
        try {
            // Create the trait using a factory, or default to an un-typed modeled trait.
            Trait createdTrait = traitFactory.createTrait(traitId, shapeBuilder.getId(), traitValue)
                    .orElseGet(() -> new DynamicTrait(traitId, traitValue));
            shapeBuilder.addTrait(createdTrait);
        } catch (SourceException e) {
            events.add(ValidationEvent.fromSourceException(e, format("Error creating trait `%s`: ",
                            Trait.getIdiomaticTraitName(traitId)))
                    .toBuilder()
                    .shapeId(shapeBuilder.getId())
                    .build());
        }
    }
}

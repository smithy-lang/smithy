/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Base class used for mutable model files.
 */
abstract class AbstractMutableModelFile implements ModelFile {

    protected final TraitContainer traitContainer;

    // A LinkedHashMap is used to maintain member order.
    private final Map<ShapeId, AbstractShapeBuilder<?, ?>> shapes = new LinkedHashMap<>();
    private final List<ValidationEvent> events = new ArrayList<>();
    private final MetadataContainer metadata = new MetadataContainer(events);
    private final TraitFactory traitFactory;

    /**
     * @param traitFactory Factory used to create traits when merging traits.
     */
    AbstractMutableModelFile(TraitFactory traitFactory) {
        this.traitFactory = Objects.requireNonNull(traitFactory, "traitFactory must not be null");
        traitContainer = new TraitContainer.TraitHashMap(traitFactory, events);
    }

    /**
     * Adds a shape to the ModelFile, checking for conflicts with other shapes.
     *
     * @param builder Shape builder to register.
     */
    void onShape(AbstractShapeBuilder<?, ?> builder) {
        if (shapes.containsKey(builder.getId())) {
            AbstractShapeBuilder<?, ?> previous = shapes.get(builder.getId());
            // Duplicate shapes in the same model file are not allowed.
            ValidationEvent event = LoaderUtils.onShapeConflict(builder.getId(), builder.getSourceLocation(),
                                                                previous.getSourceLocation());
            throw new SourceException(event.getMessage(), event.getSourceLocation());
        }

        shapes.put(builder.getId(), builder);
    }

    /**
     * Adds metadata to be reported by the ModelFile.
     *
     * @param key Metadata key to set.
     * @param value Metadata value to set.
     */
    final void putMetadata(String key, Node value) {
        metadata.putMetadata(key, value);
    }

    /**
     * Invoked when a trait is to be reported by the ModelFile.
     *
     * @param target The shape the trait is applied to.
     * @param trait The trait shape ID.
     * @param value The node value of the trait.
     */
    final void onTrait(ShapeId target, ShapeId trait, Node value) {
        traitContainer.onTrait(target, trait, value);
    }

    /**
     * Invoked when a trait is to be reported by the ModelFile.
     *
     * @param target The shape the trait is applied to.
     * @param trait The trait to apply to the shape.
     */
    final void onTrait(ShapeId target, Trait trait) {
        traitContainer.onTrait(target, trait);
    }

    @Override
    public final List<ValidationEvent> events() {
        return events;
    }

    @Override
    public final Map<String, Node> metadata() {
        return metadata.getData();
    }

    @Override
    public final Set<ShapeId> shapeIds() {
        return shapes.keySet();
    }

    @Override
    public final Collection<Shape> createShapes(TraitContainer resolvedTraits) {
        List<Shape> resolved = new ArrayList<>(shapes.size());

        // Build members and add them to top-level shapes.
        for (AbstractShapeBuilder<?, ?> builder : shapes.values()) {
            if (builder instanceof MemberShape.Builder) {
                ShapeId id = builder.getId();
                AbstractShapeBuilder<?, ?> container = shapes.get(id.withoutMember());
                if (container == null) {
                    throw new RuntimeException("Container shape not found for member: " + id);
                }
                for (Trait trait : resolvedTraits.getTraitsForShape(id).values()) {
                    builder.addTrait(trait);
                }
                container.addMember((MemberShape) builder.build());
            }
        }

        // Build top-level shapes.
        for (AbstractShapeBuilder<?, ?> builder : shapes.values()) {
            if (!(builder instanceof MemberShape.Builder)) {
                // Try/catch since shapes could have problems building, like an invalid Shape ID.
                try {
                    for (Trait trait : resolvedTraits.getTraitsForShape(builder.getId()).values()) {
                        builder.addTrait(trait);
                    }
                    resolved.add(builder.build());
                } catch (SourceException e) {
                    events.add(ValidationEvent.fromSourceException(e).toBuilder()
                                       .shapeId(builder.getId()).build());
                }
            }
        }

        return resolved;
    }

    @Override
    public final ShapeType getShapeType(ShapeId id) {
        return shapes.containsKey(id) ? shapes.get(id).getShapeType() : null;
    }
}

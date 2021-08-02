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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Aggregates together multiple {@code ModelFile}s.
 */
final class CompositeModelFile implements ModelFile {

    private static final Logger LOGGER = Logger.getLogger(CompositeModelFile.class.getName());

    private final TraitFactory traitFactory;
    private final List<ModelFile> modelFiles;
    private final List<ValidationEvent> events = new ArrayList<>();

    CompositeModelFile(TraitFactory traitFactory, List<ModelFile> modelFiles) {
        this.traitFactory = traitFactory;
        this.modelFiles = modelFiles;
    }

    @Override
    public Set<ShapeId> shapeIds() {
        Set<ShapeId> ids = new HashSet<>();
        for (ModelFile modelFile : modelFiles) {
            ids.addAll(modelFile.shapeIds());
        }
        return ids;
    }

    @Override
    public ShapeType getShapeType(ShapeId id) {
        for (ModelFile modFile : modelFiles) {
            ShapeType fileType = modFile.getShapeType(id);
            if (fileType != null) {
                return fileType;
            }
        }
        return null;
    }

    @Override
    public Map<String, Node> metadata() {
        MetadataContainer metadata = new MetadataContainer(events);
        for (ModelFile modelFile : modelFiles) {
            for (Map.Entry<String, Node> entry : modelFile.metadata().entrySet()) {
                metadata.putMetadata(entry.getKey(), entry.getValue());
            }
        }
        return metadata.getData();
    }

    @Override
    public TraitContainer resolveShapes(Set<ShapeId> ids, Function<ShapeId, ShapeType> typeProvider) {
        TraitContainer.TraitHashMap traitValues = new TraitContainer.TraitHashMap(traitFactory, events);
        for (ModelFile modelFile : modelFiles) {
            TraitContainer other = modelFile.resolveShapes(ids, typeProvider);
            for (Map.Entry<ShapeId, Map<ShapeId, Trait>> entry : other.traits().entrySet()) {
                ShapeId target = entry.getKey();
                for (Map.Entry<ShapeId, Trait> appliedEntry : entry.getValue().entrySet()) {
                    traitValues.onTrait(target, appliedEntry.getValue());
                }
            }
        }
        return traitValues;
    }

    @Override
    public List<ValidationEvent> events() {
        List<ValidationEvent> result = new ArrayList<>(events);
        for (ModelFile modelFile : modelFiles) {
            result.addAll(modelFile.events());
        }
        return result;
    }

    @Override
    public CreatedShapes createShapes(TraitContainer resolvedTraits) {
        Map<ShapeId, Shape> createdShapes = new ResolvedShapeMap();
        Map<ShapeId, PendingShape> pendingShapes = new PendingShapeMap();
        TopologicalShapeSort sorter = new TopologicalShapeSort();

        for (ModelFile modelFile : modelFiles) {
            CreatedShapes created = modelFile.createShapes(resolvedTraits);
            for (Shape shape : created.getCreatedShapes()) {
                createdShapes.put(shape.getId(), shape);
                // Optimization: Only need to add created shapes that are mixins to the queue.
                if (shape.hasTrait(MixinTrait.class)) {
                    sorter.enqueue(shape);
                }
            }
            for (PendingShape pending : created.getPendingShapes()) {
                sorter.enqueue(pending.getId(), pending.getPendingShapes());
                pendingShapes.put(pending.getId(), pending);
            }
        }

        try {
            for (ShapeId id : sorter.dequeueSortedShapes()) {
                if (pendingShapes.containsKey(id)) {
                    // Build pending shapes, which in turn populates the createShapes map.
                    pendingShapes.get(id).buildShapes(createdShapes);
                }
            }
        } catch (TopologicalShapeSort.CycleException e) {
            // Emit useful, per shape, error messages.
            for (PendingShape pending : pendingShapes.values()) {
                if (e.getUnresolved().contains(pending.getId())) {
                    events.addAll(pending.unresolved(createdShapes, pendingShapes));
                    resolvedTraits.getTraitsForShape(pending.getId()).clear();
                }
            }
        }

        return new CreatedShapes(createdShapes.values(), Collections.emptyList());
    }

    private final class ResolvedShapeMap extends HashMap<ShapeId, Shape> {
        @Override
        public Shape put(ShapeId key, Shape value) {
            Shape old = get(key);
            if (old == null) {
                return super.put(key, value);
            } else if (!old.equals(value)) {
                events.add(LoaderUtils.onShapeConflict(key, value.getSourceLocation(), old.getSourceLocation()));
            } else if (!LoaderUtils.isSameLocation(value, old)) {
                LOGGER.warning(() -> "Ignoring duplicate but equivalent shape definition: " + old.getId()
                                     + " defined at " + value.getSourceLocation() + " and "
                                     + old.getSourceLocation());
            }
            return old;
        }
    }

    private static final class PendingShapeMap extends HashMap<ShapeId, PendingShape> {
        @Override
        public PendingShape put(ShapeId key, PendingShape pending) {
            PendingShape old = get(key);
            if (old != null) {
                pending = PendingShape.mergeIntoLeft(old, pending);
            }
            return super.put(key, pending);
        }
    }
}

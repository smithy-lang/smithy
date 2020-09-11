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
    private final List<ValidationEvent> mergeEvents = new ArrayList<>();

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
        MetadataContainer metadata = new MetadataContainer(mergeEvents);
        for (ModelFile modelFile : modelFiles) {
            for (Map.Entry<String, Node> entry : modelFile.metadata().entrySet()) {
                metadata.putMetadata(entry.getKey(), entry.getValue());
            }
        }
        return metadata.getData();
    }

    @Override
    public TraitContainer resolveShapes(Set<ShapeId> ids, Function<ShapeId, ShapeType> typeProvider) {
        TraitContainer.TraitHashMap traitValues = new TraitContainer.TraitHashMap(traitFactory, mergeEvents);
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
    public Collection<Shape> createShapes(TraitContainer resolvedTraits) {
        // Merge all shapes together, resolve conflicts, and warn for acceptable conflicts.
        Map<ShapeId, Shape> shapes = new HashMap<>();
        for (ModelFile modelFile : modelFiles) {
            for (Shape shape : modelFile.createShapes(resolvedTraits)) {
                Shape previous = shapes.get(shape.getId());
                if (previous == null) {
                    shapes.put(shape.getId(), shape);
                } else if (!previous.equals(shape)) {
                    mergeEvents.add(LoaderUtils.onShapeConflict(shape.getId(), shape.getSourceLocation(),
                                                                previous.getSourceLocation()));
                } else if (!LoaderUtils.isSameLocation(shape, previous)) {
                    LOGGER.warning(() -> "Ignoring duplicate but equivalent shape definition: " + previous.getId()
                                         + " defined at " + shape.getSourceLocation() + " and "
                                         + previous.getSourceLocation());
                }
            }
        }

        return shapes.values();
    }

    @Override
    public List<ValidationEvent> events() {
        List<ValidationEvent> events = new ArrayList<>(mergeEvents);
        for (ModelFile modelFile : modelFiles) {
            events.addAll(modelFile.events());
        }
        return events;
    }
}

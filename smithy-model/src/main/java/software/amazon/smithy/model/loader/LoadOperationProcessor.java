/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;

final class LoadOperationProcessor implements Consumer<LoadOperation> {

    private final List<ValidationEvent> events;
    private final MetadataContainer metadata = new MetadataContainer();
    private final LoaderShapeMap shapeMap;
    private final LoaderTraitMap traitMap;
    private final Queue<LoadOperation.ForwardReference> forwardReferences = new ArrayDeque<>();
    private final LoadOperation.Visitor visitor;
    private final Model prelude;
    private final Map<String, Version> modelVersions = new HashMap<>();

    LoadOperationProcessor(
            TraitFactory traitFactory,
            Model prelude,
            boolean allowUnknownTraits,
            Consumer<ValidationEvent> validationEventListener,
            ValidationEventDecorator decorator
    ) {
        // Emit events as the come in.
        this.events = new ArrayList<ValidationEvent>() {
            @Override
            public boolean add(ValidationEvent e) {
                e = decorator.decorate(e);
                validationEventListener.accept(e);
                return super.add(e);
            }

            @Override
            public boolean addAll(Collection<? extends ValidationEvent> validationEvents) {
                ensureCapacity(size() + validationEvents.size());
                for (ValidationEvent e : validationEvents) {
                    add(e);
                }
                return true;
            }
        };

        this.prelude = prelude;
        shapeMap = new LoaderShapeMap(prelude, events);
        traitMap = new LoaderTraitMap(traitFactory, events, allowUnknownTraits);

        this.visitor = new LoadOperation.Visitor() {
            @Override
            public void putMetadata(LoadOperation.PutMetadata operation) {
                metadata.putMetadata(operation.key, operation.value, events);
            }

            @Override
            public void applyTrait(LoadOperation.ApplyTrait operation) {
                traitMap.add(operation);
                shapeMap.moveCreatedShapeToOperations(operation.target, LoadOperationProcessor.this);
            }

            @Override
            public void defineShape(LoadOperation.DefineShape operation) {
                shapeMap.add(operation);
                shapeMap.moveCreatedShapeToOperations(operation.toShapeId(), LoadOperationProcessor.this);
            }

            @Override
            public void forwardReference(LoadOperation.ForwardReference operation) {
                forwardReferences.add(operation);
            }

            @Override
            public void event(LoadOperation.Event operation) {
                events.add(operation.event);
            }

            @Override
            public void modelVersion(LoadOperation.ModelVersion operation) {
                // Don't attempt to assign versions based on N/A or "" source locations.
                if (!operation.getSourceLocation().equals(SourceLocation.none())
                        && !operation.getSourceLocation().getFilename().isEmpty()) {
                    modelVersions.put(operation.getSourceLocation().getFilename(), operation.version);
                }
            }
        };
    }

    @Override
    public void accept(LoadOperation operation) {
        operation.accept(visitor);
    }

    void putCreatedShape(Shape shape) {
        shapeMap.add(shape, this);
    }

    Version getShapeVersion(Shape shape) {
        SourceLocation location = shape.getSourceLocation();
        // Nodes might have no sourcelocation or an empty filename.
        if (location == SourceLocation.NONE || location.getFilename().isEmpty()) {
            return shapeMap.getShapeVersion(shape.getId());
        } else {
            return modelVersions.getOrDefault(location.getFilename(), Version.UNKNOWN);
        }
    }

    Model buildModel() {
        Model.Builder modelBuilder = Model.builder();
        modelBuilder.metadata(metadata.getData());
        resolveForwardReferences();
        traitMap.applyTraitsToNonMixinsInShapeMap(shapeMap);
        shapeMap.buildShapesAndClaimMixinTraits(modelBuilder, traitMap::claimTraitsForShape);
        traitMap.emitUnclaimedTraits();
        if (prelude != null) {
            modelBuilder.addShapes(prelude);
        }
        return modelBuilder.build();
    }

    List<ValidationEvent> events() {
        return events;
    }

    private void resolveForwardReferences() {
        while (!forwardReferences.isEmpty()) {
            LoadOperation.ForwardReference reference = forwardReferences.poll();
            if (reference.namespace == null) {
                // Assume smithy.api if there is no namespace. This can happen in metadata and control sections.
                ShapeId absolute = ShapeId.fromOptionalNamespace(Prelude.NAMESPACE, reference.name);
                resolveReference(reference, absolute, shapeMap.getShapeType(absolute));
            } else {
                detectAndEmitForwardReference(reference);
            }
        }
    }

    private void resolveReference(LoadOperation.ForwardReference reference, ShapeId id, ShapeType type) {
        ValidationEvent event = reference.resolve(id, type);
        if (event != null) {
            events.add(event);
        }
    }

    private void detectAndEmitForwardReference(LoadOperation.ForwardReference reference) {
        Objects.requireNonNull(reference.namespace);
        ShapeId inNamespace = ShapeId.fromOptionalNamespace(reference.namespace, reference.name);
        ShapeType inNamespaceType = shapeMap.getShapeType(inNamespace);

        if (inNamespaceType != null) {
            resolveReference(reference, inNamespace, inNamespaceType);
        } else {
            // Try to find a prelude shape by ID if no ID exists in the namespace with this name.
            ShapeId preludeId = ShapeId.fromOptionalNamespace(Prelude.NAMESPACE, reference.name);
            if (prelude != null && prelude.getShapeIds().contains(preludeId)) {
                resolveReference(reference, preludeId, prelude.expectShape(preludeId).getType());
            } else {
                resolveReference(reference, inNamespace, null);
            }
        }
    }
}

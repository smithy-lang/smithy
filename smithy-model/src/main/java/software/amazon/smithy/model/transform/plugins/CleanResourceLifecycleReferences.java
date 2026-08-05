/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractResourceLifecycleTrait;
import software.amazon.smithy.model.traits.CreatesResourcesTrait;
import software.amazon.smithy.model.traits.DeletesResourcesTrait;
import software.amazon.smithy.model.traits.PutsResourcesTrait;
import software.amazon.smithy.model.traits.ReadsResourcesTrait;
import software.amazon.smithy.model.traits.ResourceLifecycleBinding;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UpdatesResourcesTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Removes references to resources that are removed from resource lifecycle
 * traits ({@code @createsResources}, {@code @deletesResources},
 * {@code @putsResources}, {@code @readsResources}, {@code @updatesResources}).
 */
@SmithyUnstableApi
public final class CleanResourceLifecycleReferences implements ModelTransformerPlugin {

    private static final List<ShapeId> LIFECYCLE_TRAITS = ListUtils.of(
            CreatesResourcesTrait.ID,
            DeletesResourcesTrait.ID,
            PutsResourcesTrait.ID,
            ReadsResourcesTrait.ID,
            UpdatesResourcesTrait.ID);

    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> shapes, Model model) {
        Set<ShapeId> removedIds = new HashSet<>();
        for (Shape shape : shapes) {
            removedIds.add(shape.getId());
        }

        Set<Shape> toReplace = new HashSet<>();
        for (OperationShape operation : model.getOperationShapes()) {
            OperationShape.Builder builder = null;
            for (ShapeId traitId : LIFECYCLE_TRAITS) {
                builder = cleanTrait(operation, builder, removedIds, traitId);
            }
            if (builder != null) {
                toReplace.add(builder.build());
            }
        }

        return toReplace.isEmpty() ? model : transformer.replaceShapes(model, toReplace);
    }

    private OperationShape.Builder cleanTrait(
            OperationShape operation,
            OperationShape.Builder builder,
            Set<ShapeId> removedIds,
            ShapeId traitId
    ) {
        Optional<Trait> traitOptional = operation.findTrait(traitId);
        if (!traitOptional.isPresent()) {
            return builder;
        }

        AbstractResourceLifecycleTrait lifecycleTrait = (AbstractResourceLifecycleTrait) traitOptional.get();
        List<ResourceLifecycleBinding> bindings = lifecycleTrait.getBindings();
        List<ResourceLifecycleBinding> filtered = new ArrayList<>(bindings.size());
        for (ResourceLifecycleBinding binding : bindings) {
            if (!removedIds.contains(binding.getResource())) {
                filtered.add(binding);
            }
        }

        if (filtered.size() == bindings.size()) {
            return builder; // Nothing removed, no change needed.
        }

        OperationShape.Builder result = builder != null ? builder : operation.toBuilder();
        if (filtered.isEmpty()) {
            result.removeTrait(traitId);
        } else {
            result.addTrait(rebuild(traitId, lifecycleTrait, filtered));
        }
        return result;
    }

    // Rebuilds the concrete lifecycle trait with the filtered bindings, preserving the
    // original source location. The dispatch lives here rather than on the trait classes.
    private Trait rebuild(
            ShapeId traitId,
            AbstractResourceLifecycleTrait original,
            List<ResourceLifecycleBinding> filtered
    ) {
        AbstractResourceLifecycleTrait.Builder<?, ?> builder;
        switch (traitId.getName()) {
            case "createsResources":
                builder = CreatesResourcesTrait.builder();
                break;
            case "deletesResources":
                builder = DeletesResourcesTrait.builder();
                break;
            case "putsResources":
                builder = PutsResourcesTrait.builder();
                break;
            case "readsResources":
                builder = ReadsResourcesTrait.builder();
                break;
            case "updatesResources":
                builder = UpdatesResourcesTrait.builder();
                break;
            default:
                throw new IllegalStateException("Unexpected resource lifecycle trait: " + traitId);
        }
        return builder.sourceLocation(original.getSourceLocation())
                .bindings(filtered)
                .build();
    }
}

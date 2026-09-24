/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import software.amazon.smithy.model.traits.ResourceBinding;
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
        if (!operation.hasTrait(traitId)) {
            return builder;
        }

        AbstractResourceLifecycleTrait<?> lifecycleTrait =
                (AbstractResourceLifecycleTrait<?>) operation.findTrait(traitId).get();
        List<? extends ResourceBinding> bindings = lifecycleTrait.getBindings();
        int remaining = 0;
        for (ResourceBinding binding : bindings) {
            if (!removedIds.contains(binding.getResource())) {
                remaining++;
            }
        }

        if (remaining == bindings.size()) {
            return builder; // Nothing removed, no change needed.
        }

        OperationShape.Builder result = builder != null ? builder : operation.toBuilder();
        if (remaining == 0) {
            result.removeTrait(traitId);
        } else {
            result.addTrait(rebuild(traitId, lifecycleTrait, removedIds));
        }
        return result;
    }

    // Rebuilds the concrete lifecycle trait with the bindings whose resource was not removed,
    // preserving the original source location. The dispatch lives here rather than on the trait
    // classes; each branch keeps the concrete binding type so delete stays properties-free.
    private Trait rebuild(
            ShapeId traitId,
            AbstractResourceLifecycleTrait<?> original,
            Set<ShapeId> removedIds
    ) {
        if (traitId.equals(CreatesResourcesTrait.ID)) {
            CreatesResourcesTrait trait = (CreatesResourcesTrait) original;
            return CreatesResourcesTrait.builder()
                    .sourceLocation(trait.getSourceLocation())
                    .bindings(retain(trait.getBindings(), removedIds))
                    .build();
        } else if (traitId.equals(PutsResourcesTrait.ID)) {
            PutsResourcesTrait trait = (PutsResourcesTrait) original;
            return PutsResourcesTrait.builder()
                    .sourceLocation(trait.getSourceLocation())
                    .bindings(retain(trait.getBindings(), removedIds))
                    .build();
        } else if (traitId.equals(ReadsResourcesTrait.ID)) {
            ReadsResourcesTrait trait = (ReadsResourcesTrait) original;
            return ReadsResourcesTrait.builder()
                    .sourceLocation(trait.getSourceLocation())
                    .bindings(retain(trait.getBindings(), removedIds))
                    .build();
        } else if (traitId.equals(UpdatesResourcesTrait.ID)) {
            UpdatesResourcesTrait trait = (UpdatesResourcesTrait) original;
            return UpdatesResourcesTrait.builder()
                    .sourceLocation(trait.getSourceLocation())
                    .bindings(retain(trait.getBindings(), removedIds))
                    .build();
        } else if (traitId.equals(DeletesResourcesTrait.ID)) {
            DeletesResourcesTrait trait = (DeletesResourcesTrait) original;
            return DeletesResourcesTrait.builder()
                    .sourceLocation(trait.getSourceLocation())
                    .bindings(retain(trait.getBindings(), removedIds))
                    .build();
        } else {
            throw new IllegalStateException("Unexpected resource lifecycle trait: " + traitId);
        }
    }

    // Returns the bindings whose resource was not removed, preserving the concrete binding type.
    private static <B extends ResourceBinding> List<B> retain(List<B> bindings, Set<ShapeId> removedIds) {
        List<B> filtered = new ArrayList<>(bindings.size());
        for (B binding : bindings) {
            if (!removedIds.contains(binding.getResource())) {
                filtered.add(binding);
            }
        }
        return filtered;
    }
}

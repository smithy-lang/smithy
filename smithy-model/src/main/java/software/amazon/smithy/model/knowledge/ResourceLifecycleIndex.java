/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.AbstractResourceLifecycleTrait;
import software.amazon.smithy.model.traits.CreatesResourcesTrait;
import software.amazon.smithy.model.traits.DeletesResourcesTrait;
import software.amazon.smithy.model.traits.PutsResourcesTrait;
import software.amazon.smithy.model.traits.ReadsResourcesTrait;
import software.amazon.smithy.model.traits.ResourceLifecycleBinding;
import software.amazon.smithy.model.traits.UpdatesResourcesTrait;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Index that provides efficient lookups for resource lifecycle relationships
 * declared via {@code @createsResources}, {@code @deletesResources},
 * {@code @putsResources}, {@code @readsResources}, and {@code @updatesResources}.
 *
 * <p>This index enables two primary query patterns:
 * <ul>
 *   <li>Given an operation, what resources does it affect (by lifecycle type)?</li>
 *   <li>Given a resource, what operations affect it (by lifecycle type)?</li>
 * </ul>
 *
 * <p>This index only covers relationships declared via the lifecycle traits.
 * It does not include the standard 1:1Lifecycle bindings on resource shapes
 * (create, put, read, update, delete). Use {@link BottomUpIndex} or
 * {@link ResourceShape} directly for those.
 */
@SmithyUnstableApi
public final class ResourceLifecycleIndex implements KnowledgeIndex {

    /**
     * The lifecycle effect an operation has on a resource.
     */
    public enum Lifecycle {
        CREATE,
        DELETE,
        PUT,
        READ,
        UPDATE
    }

    // operation -> lifecycle -> set of resource ShapeIds
    private final Map<ShapeId, Map<Lifecycle, Set<ShapeId>>> operationToResources = new HashMap<>();

    // resource -> lifecycle -> set of operation ShapeIds
    private final Map<ShapeId, Map<Lifecycle, Set<ShapeId>>> resourceToOperations = new HashMap<>();

    private ResourceLifecycleIndex(Model model) {
        Map<ShapeId, Lifecycle> traitToLifecycle = new HashMap<>();
        traitToLifecycle.put(CreatesResourcesTrait.ID, Lifecycle.CREATE);
        traitToLifecycle.put(DeletesResourcesTrait.ID, Lifecycle.DELETE);
        traitToLifecycle.put(PutsResourcesTrait.ID, Lifecycle.PUT);
        traitToLifecycle.put(ReadsResourcesTrait.ID, Lifecycle.READ);
        traitToLifecycle.put(UpdatesResourcesTrait.ID, Lifecycle.UPDATE);

        for (OperationShape operation : model.getOperationShapes()) {
            for (Map.Entry<ShapeId, Lifecycle> entry : traitToLifecycle.entrySet()) {
                operation.findTrait(entry.getKey())
                        .ifPresent(trait -> index(operation.getId(),
                                entry.getValue(),
                                ((AbstractResourceLifecycleTrait) trait).getBindings()));
            }
        }
    }

    public static ResourceLifecycleIndex of(Model model) {
        return model.getKnowledge(ResourceLifecycleIndex.class, ResourceLifecycleIndex::new);
    }

    /**
     * Gets the resources affected by an operation for a specific lifecycle type.
     *
     * @param operation The operation to query.
     * @param lifecycle The lifecycle type to filter by.
     * @return The set of resource shape IDs, or empty if none.
     */
    public Set<ShapeId> getResources(ToShapeId operation, Lifecycle lifecycle) {
        return Collections.unmodifiableSet(operationToResources
                .getOrDefault(operation.toShapeId(), Collections.emptyMap())
                .getOrDefault(lifecycle, Collections.emptySet()));
    }

    /**
     * Gets all resources affected by an operation across all lifecycle types.
     *
     * @param operation The operation to query.
     * @return A map of lifecycle type to the set of affected resource shape IDs.
     */
    public Map<Lifecycle, Set<ShapeId>> getResources(ToShapeId operation) {
        return Collections.unmodifiableMap(
                operationToResources.getOrDefault(operation.toShapeId(), Collections.emptyMap()));
    }

    /**
     * Gets the operations that affect a resource for a specific lifecycle type.
     *
     * @param resource The resource to query.
     * @param lifecycle The lifecycle type to filter by.
     * @return The set of operation shape IDs, or empty if none.
     */
    public Set<ShapeId> getOperations(ToShapeId resource, Lifecycle lifecycle) {
        return Collections.unmodifiableSet(resourceToOperations
                .getOrDefault(resource.toShapeId(), Collections.emptyMap())
                .getOrDefault(lifecycle, Collections.emptySet()));
    }

    /**
     * Gets all operations that affect a resource across all lifecycle types.
     *
     * @param resource The resource to query.
     * @return A map of lifecycle type to the set of operation shape IDs.
     */
    public Map<Lifecycle, Set<ShapeId>> getOperations(ToShapeId resource) {
        return Collections.unmodifiableMap(
                resourceToOperations.getOrDefault(resource.toShapeId(), Collections.emptyMap()));
    }

    private void index(ShapeId operationId, Lifecycle lifecycle, List<ResourceLifecycleBinding> bindings) {
        for (ResourceLifecycleBinding binding : bindings) {
            ShapeId resourceId = binding.getResource();

            operationToResources
                    .computeIfAbsent(operationId, k -> new HashMap<>())
                    .computeIfAbsent(lifecycle, k -> new LinkedHashSet<>())
                    .add(resourceId);

            resourceToOperations
                    .computeIfAbsent(resourceId, k -> new HashMap<>())
                    .computeIfAbsent(lifecycle, k -> new LinkedHashSet<>())
                    .add(operationId);
        }
    }
}

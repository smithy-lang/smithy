/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.knowledge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.ResourceIdentifierTrait;
import software.amazon.smithy.utils.Pair;

/**
 * Index of operation shapes to the identifiers bound to the operation.
 */
public final class IdentifierBindingIndex implements KnowledgeIndex {
    /** Map of Resource shape ID to a map of Operation shape ID to a map of identifier name to the member name. */
    private final Map<ShapeId, Map<ShapeId, Map<String, String>>> inputBindings = new HashMap<>();
    private final Map<ShapeId, Map<ShapeId, Map<String, String>>> outputBindings = new HashMap<>();
    private final SortedSet<String> allIdentifiers = new TreeSet<>();

    /** Map of Resource shape ID to a map of Operation shape ID to a binding type. */
    private final Map<ShapeId, Map<ShapeId, BindingType>> bindingTypes = new HashMap<>();

    public enum BindingType {
        /** Indicates that the operation is bound to a resource as an instance operation. */
        INSTANCE,
        /** Indicates that the operation is bound to a resource as a collection operation. */
        COLLECTION,
        /** Indicates that the operation is not bound to a resource. */
        NONE
    }

    public IdentifierBindingIndex(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);
        for (ResourceShape resource : model.getResourceShapes()) {
            processResource(resource, operationIndex, model);
        }
    }

    public static IdentifierBindingIndex of(Model model) {
        return model.getKnowledge(IdentifierBindingIndex.class, IdentifierBindingIndex::new);
    }

    /**
     * Gets the identifier binding type of an operation to a resource.
     *
     * <p>The NONE Binding type is returned if the resource can't be found,
     * the operation can't be found, or if the operation is not bound to the
     * resource.
     *
     * @param resource Shape ID of a resource.
     * @param operation Shape ID of an operation.
     * @return Returns the binding type of the operation.
     */
    public BindingType getOperationBindingType(ToShapeId resource, ToShapeId operation) {
        return Optional.ofNullable(bindingTypes.get(resource.toShapeId()))
                .flatMap(resourceMap -> Optional.ofNullable(resourceMap.get(operation.toShapeId())))
                .orElse(BindingType.NONE);
    }

    /**
     * Gets a map of identifier names to input member names that provide a
     * value for that identifier.
     *
     * @param resource Shape ID of a resource.
     * @param operation Shape ID of an operation.
     * @return Returns the identifier bindings map or an empty map if the
     *  binding is invalid or cannot be found.
     */
    public Map<String, String> getOperationInputBindings(ToShapeId resource, ToShapeId operation) {
        return Optional.ofNullable(inputBindings.get(resource.toShapeId()))
                .flatMap(resourceMap -> Optional.ofNullable(resourceMap.get(operation.toShapeId())))
                .map(Collections::unmodifiableMap)
                .orElseGet(Collections::emptyMap);
    }

    /**
     * Gets a map of identifier names to output member names that provide a
     * value for that identifier.
     *
     * @param resource Shape ID of a resource.
     * @param operation Shape ID of an operation.
     * @return Returns the identifier bindings map or an empty map if the
     *  binding is invalid or cannot be found.
     */
    public Map<String, String> getOperationOutputBindings(ToShapeId resource, ToShapeId operation) {
        return Optional.ofNullable(outputBindings.get(resource.toShapeId()))
                .flatMap(resourceMap -> Optional.ofNullable(resourceMap.get(operation.toShapeId())))
                .map(Collections::unmodifiableMap)
                .orElseGet(Collections::emptyMap);
    }

    /**
     * Gets a map of identifier names to input member names that provide a
     * value for that identifier.
     *
     * @deprecated Use {@link #getOperationInputBindings} instead.
     *
     * @param resource Shape ID of a resource.
     * @param operation Shape ID of an operation.
     * @return Returns the identifier bindings map or an empty map if the
     *  binding is invalid or cannot be found.
     */
    @Deprecated
    public Map<String, String> getOperationBindings(ToShapeId resource, ToShapeId operation) {
        return getOperationInputBindings(resource, operation);
    }

    private void processResource(ResourceShape resource, OperationIndex operationIndex, Model model) {
        inputBindings.put(resource.getId(), new HashMap<>());
        outputBindings.put(resource.getId(), new HashMap<>());
        bindingTypes.put(resource.getId(), new HashMap<>());
        resource.getAllOperations().forEach(operationId -> {
            // Ignore broken models in this index.
            Map<String, String> computedInputBindings = operationIndex.getInputShape(operationId)
                    .map(inputShape -> computeBindings(resource, inputShape))
                    .orElse(Collections.emptyMap());
            inputBindings.get(resource.getId()).put(operationId, computedInputBindings);
            allIdentifiers.addAll(computedInputBindings.keySet());

            Map<String, String> computedOutputBindings = operationIndex.getOutputShape(operationId)
                    .map(outputShape -> computeBindings(resource, outputShape))
                    .orElse(Collections.emptyMap());
            outputBindings.get(resource.getId()).put(operationId, computedOutputBindings);
            allIdentifiers.addAll(computedOutputBindings.keySet());

            bindingTypes.get(resource.getId()).put(operationId, isCollection(resource, operationId)
                    ? BindingType.COLLECTION
                    : BindingType.INSTANCE);
        });
    }

    private boolean isCollection(ResourceShape resource, ToShapeId operationId) {
        return resource.getCollectionOperations().contains(operationId)
                || (resource.getCreate().isPresent() && resource.getCreate().get().toShapeId().equals(operationId))
                || (resource.getList().isPresent() && resource.getList().get().toShapeId().equals(operationId));
    }

    private boolean isImplicitIdentifierBinding(Map.Entry<String, MemberShape> memberEntry, ResourceShape resource) {
        return resource.getIdentifiers().containsKey(memberEntry.getKey())
                && memberEntry.getValue().getTrait(RequiredTrait.class).isPresent()
                && memberEntry.getValue().getTarget().equals(resource.getIdentifiers().get(memberEntry.getKey()));
    }

    private Map<String, String> computeBindings(ResourceShape resource, StructureShape shape) {
        return shape.getAllMembers().entrySet().stream()
                .flatMap(entry -> entry.getValue().getTrait(ResourceIdentifierTrait.class)
                        .map(trait -> Stream.of(Pair.of(trait.getValue(), entry.getKey())))
                        .orElseGet(() -> isImplicitIdentifierBinding(entry, resource)
                                ? Stream.of(Pair.of(entry.getKey(), entry.getKey()))
                                : Stream.empty()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

    }
}

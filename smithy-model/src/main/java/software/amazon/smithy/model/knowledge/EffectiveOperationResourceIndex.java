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

package software.amazon.smithy.model.knowledge;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.MapUtils;

/**
 * Computes and indexes the resources on which a service's operations may be thought to act.
 *
 * TODO: Add better description.
 */
public final class EffectiveOperationResourceIndex implements KnowledgeIndex {
    private final Map<ShapeId, Map<ShapeId, EffectiveResourceBindings>> bindings;

    public EffectiveOperationResourceIndex(Model model) {
        ShapeIndex shapeIndex = model.getShapeIndex();
        TopDownIndex topDownIndex = model.getKnowledge(TopDownIndex.class);
        IdentifierBindingIndex identifierBindingIndex = model.getKnowledge(IdentifierBindingIndex.class);

        bindings = shapeIndex.shapes(ServiceShape.class)
                .map(serviceShape -> Pair.of(
                        serviceShape.getId(),
                        compileEffectiveResources(topDownIndex, identifierBindingIndex, serviceShape)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Map<ShapeId, EffectiveResourceBindings> compileEffectiveResources(
            TopDownIndex topDown,
            IdentifierBindingIndex ids,
            ServiceShape service
    ) {
        return topDown.getContainedResources(service).stream()
                .flatMap(resource -> resource.getAllOperations().stream()
                        .flatMap(operationId -> getEffectiveResourceTarget(
                                ids, topDown, resource, operationId, service).stream())
                        .map(pair -> Pair.of(pair.getLeft(), new EffectiveResourceBindings(
                                pair.getRight(),
                                ids.getOperationBindings(resource, pair.getLeft())))))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Optional<Pair<ShapeId, ResourceShape>> getEffectiveResourceTarget(
            IdentifierBindingIndex identifierBindingIndex,
            TopDownIndex topDownIndex,
            ResourceShape resource,
            ShapeId operationId,
            ToShapeId serviceOrId
    ) {
        if (identifierBindingIndex.getOperationBindingType(resource, operationId)
                == IdentifierBindingIndex.BindingType.COLLECTION) {
            return topDownIndex.getContainedResources(serviceOrId).stream()
                    .filter(resourceShape -> resourceShape.getResources().contains(resource.getId()))
                    .findFirst()
                    .map(parentResource -> Pair.of(operationId, parentResource));
        }

        return Optional.of(Pair.of(operationId, resource));
    }

    /**
     * Determine the resource against which an operation may be thought to
     * operate as well as the identifier bindings of the latter on the former.
     *
     * For instance operations, the effective resource will be the resource to
     * which the operation is bound. For collection operations, the effective
     * resource will be the bound resource's parent (if any).
     *
     * @param service A service shape or the ShapeId thereof.
     * @param operation An operation shape or the ShapeId thereof.
     * @return The resource on which the identified operation acts and a map of
     *          resource identifier names to operation input member names. For
     *          operations that are not bound to a resource (or collection
     *          operations bound to a top-level resource), an empty optional
     *          will be returned.
     */
    public Optional<EffectiveResourceBindings> getEffectiveResourceAndBindings(
            ToShapeId service,
            ToShapeId operation
    ) {
        return Optional.ofNullable(bindings.get(service.toShapeId()))
                .flatMap(map -> Optional.ofNullable(map.get(operation.toShapeId())));
    }

    /**
     * Determine the resource against which an operation may be thought to
     * operate.
     *
     * This method is a shorthand for calling
     * {@link #getEffectiveResourceAndBindings(ToShapeId, ToShapeId)} and then
     * calling {@link EffectiveResourceBindings#getResource()} on the result.
     *
     * @param service A service shape or the ShapeId thereof.
     * @param operation An operation shape or the ShapeId thereof.
     * @return The resource on which the identified operation acts. For
     *          operations that are not bound to a resource (or collection
     *          operations bound to a top-level resource), an empty optional
     *          will be returned.
     */
    public Optional<ResourceShape> getEffectiveResource(ToShapeId service, ToShapeId operation) {
        return getEffectiveResourceAndBindings(service, operation)
                .map(EffectiveResourceBindings::getResource);
    }

    public static final class EffectiveResourceBindings {
        private final ResourceShape resource;
        private final Map<String, String> bindings;

        private EffectiveResourceBindings(ResourceShape resource, Map<String, String> bindings) {
            this.resource = resource;
            this.bindings = MapUtils.copyOf(bindings);
        }

        public ResourceShape getResource() {
            return resource;
        }

        public Map<String, String> getBindings() {
            return bindings;
        }
    }
}

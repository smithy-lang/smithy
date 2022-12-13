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

package software.amazon.smithy.aws.traits.tagging;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.PropertyBindingIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Index of AWS tagging trait information in a service closure and convenient
 * access to tag operations by name in service closures.
 */
@SmithyUnstableApi
public final class AwsTagIndex implements KnowledgeIndex {
    private final Set<ShapeId> servicesWithAllTagOperations = new HashSet<>();
    private final Set<ShapeId> resourceIsTagOnCreate = new HashSet<>();
    private final Set<ShapeId> resourceIsTagOnUpdate = new HashSet<>();
    private final Map<ShapeId, ShapeId> shapeToTagOperation = new HashMap<>();
    private final Set<ShapeId> serviceTagOperationIsValid = new HashSet<>();
    private final Map<ShapeId, ShapeId> shapeToUntagOperation = new HashMap<>();
    private final Set<ShapeId> serviceUntagOperationIsValid = new HashSet<>();
    private final Map<ShapeId, ShapeId> shapeToListTagsOperation = new HashMap<>();
    private final Set<ShapeId> serviceListTagsOperationIsValid = new HashSet<>();

    private AwsTagIndex(Model model) {
        PropertyBindingIndex propertyBindingIndex = PropertyBindingIndex.of(model);
        OperationIndex operationIndex = OperationIndex.of(model);

        for (ServiceShape service : model.getServiceShapesWithTrait(TagEnabledTrait.class)) {
            for (ShapeId resourceId : service.getResources()) {
                ResourceShape resource = model.expectShape(resourceId, ResourceShape.class);
                if (resource.hasTrait(TaggableTrait.class)
                        && resource.expectTrait(TaggableTrait.class).getProperty().isPresent()) {

                    // Check if tag property is specified on create.
                    if (TaggingShapeUtils.isTagPropertyInInput(resource.getCreate(), model, resource,
                            propertyBindingIndex)) {
                        resourceIsTagOnCreate.add(resourceId);
                    }
                    // Check if tag property is specified on update.
                    if (TaggingShapeUtils.isTagPropertyInInput(resource.getUpdate(), model, resource,
                            propertyBindingIndex)) {
                        resourceIsTagOnUpdate.add(resourceId);
                    }
                }
            }
            //Check if service has the three service-wide tagging operations unbound to any resource.
            if (verifyTagApis(model, service, operationIndex)) {
                servicesWithAllTagOperations.add(service.getId());
            }
        }
    }

    public static AwsTagIndex of(Model model) {
        return new AwsTagIndex(model);
    }

    /**
     * Checks if the given ShapeID references a resource shape that meets tag on create criteria.
     * If the given ShapeID does not reference a resource shape, will always return false.
     *
     * Tag on create is satisfied when a resource shape has a property representing tags via
     * the {@see TaggableTrait}, and that property is an input on the resource's create lifecycle
     * operation.
     *
     * @param resourceId ShapeID of the resource to check.
     * @return true iff the resourceId references a resource shape that has tag-on create behavior.
     */
    public boolean isResourceTagOnCreate(ShapeId resourceId) {
        return resourceIsTagOnCreate.contains(resourceId);
    }

    /**
     * Checks if the given ShapeID references a resource shape that meets tag on update criteria.
     * If the given ShapeID does not reference to a resource shape, will always return false.
     *
     * Tag on update is satisfied when a resource shape has a property representing tags via
     * the {@see TaggableTrait}, and that property is an input on the resource's create lifecycle
     * operation.
     *
     * @param resourceId ShapeID of the resource to check.
     * @return true iff the resourceId references a resource shape that has tag-on update behavior.
     */
    public boolean isResourceTagOnUpdate(ShapeId resourceId) {
        return resourceIsTagOnUpdate.contains(resourceId);
    }

    /**
     * Checks if a given ShapeID references a service shape that meets the criteria for having the three
     * expected service-wide tagging APIs.
     *
     * @param serviceShapeId ShapeID of the shape to check.
     * @return true iff the serviceShapeId references a service shape that has the necessary tagging APIs.
     */
    public boolean serviceHasTagApis(ShapeId serviceShapeId) {
        return servicesWithAllTagOperations.contains(serviceShapeId);
    }

    /**
     * Gets the ShapeID of the TagResource operation on the shape if one is found by name.
     * If a resource ID is passed in, it will return the qualifiying service wide TagResource operation ID rather than
     * the operation ID specified by the tagApi property.
     *
     * @param serviceOrResourceId ShapeID of the service shape to retrieve the qualifying TagResource operation for.
     * @return The ShapeID of a qualifying TagResource operation if one is found. Returns an empty optional otherwise.
     */
    public Optional<ShapeId> getTagResourceOperation(ShapeId serviceOrResourceId) {
        return Optional.ofNullable(shapeToTagOperation.get(serviceOrResourceId));
    }

    /**
     * Gets the ShapeID of the UntagResource operation on the shape if one is found meeting the criteria.
     * If a resource ID is passed in, it will return the qualifiying service wide TagResource operation ID rather than
     * the operation ID specified by the tagApi property.
     *
     * @param serviceOrResourceId ShapeID of the service shape to retrieve the qualifying UntagResource operation for.
     * @return The ShapeID of a qualifying UntagResource operation if one is found. Returns an empty optional
     *   otherwise.
     */
    public Optional<ShapeId> getUntagResourceOperation(ShapeId serviceOrResourceId) {
        return Optional.ofNullable(shapeToUntagOperation.get(serviceOrResourceId));
    }

    /**
     * Gets the ShapeID of the ListTagsForResource operation on the service shape if one is found meeting the criteria.
     * If a resource ID is passed in, it will return the qualifiying service wide TagResource operation ID rather than
     * the operation ID specified by the tagApi property.
     *
     * @param serviceOrResourceId ShapeID of the service shape to retrieve the qualifying ListTagsForResource
     *  operation for.
     * @return The ShapeID of a qualifying ListTagsForResource operation if one is found. Returns an empty optional
     *  otherwise.
     */
    public Optional<ShapeId> getListTagsForResourceOperation(ShapeId serviceOrResourceId) {
        return Optional.ofNullable(shapeToListTagsOperation.get(serviceOrResourceId));
    }

    /**
     * Gets the verification result of whether or not the service has a named TagResource operation that meets the
     * expected critiera for TagResource.
     *
     * @param serviceId the ShapeID of the service shape the TagResource operation should be bound to.
     * @return True iff the service shape has a service bound operation named 'TagResource' that also satisfies
     *   the criteria for a valid TagResource operation.
     */
    public boolean serviceHasValidTagResourceOperation(ShapeId serviceId) {
        return serviceTagOperationIsValid.contains(serviceId);
    }

    /**
     * Gets the verification result of whether or not the service has a named UntagResource operation that meets the
     * expected critiera for UntagResource.
     *
     * @param serviceId the ShapeID of the service shape the UntagResource operation should be bound to.
     * @return True iff the service shape has a service bound operation named 'UntagResource' that also satisfies
     *   the criteria for a valid UntagResource operation.
     */
    public boolean serviceHasValidUntagResourceOperation(ShapeId serviceId) {
        return serviceUntagOperationIsValid.contains(serviceId);
    }

    /**
     * Gets the verification result of whether or not the service has a named ListTagsForResource operation that meets
     * the expected critiera for ListTagsForResource.
     *
     * @param serviceId the ShapeID of the service shape the ListTagsForResource operation should be bound to.
     * @return True iff the service shape has a service bound operation named 'ListTagsForResource' that also satisfies
     *   the criteria for a valid ListTagsForResource operation.
     */
    public boolean serviceHasValidListTagsForResourceOperation(ShapeId serviceId) {
        return serviceListTagsOperationIsValid.contains(serviceId);
    }

    private boolean verifyTagApis(Model model, ServiceShape service, OperationIndex operationIndex) {
        boolean hasTagApi = false;
        boolean hasUntagApi = false;
        boolean hasListTagsApi = false;
        Collection<ShapeId> serviceResources = new LinkedList<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        topDownIndex.getContainedResources(service).forEach(resource -> serviceResources.add(resource.getId()));

        Map<String, ShapeId> operationMap = new HashMap<>();
        for (ShapeId operationId : service.getOperations()) {
            operationMap.put(operationId.getName(), operationId);
        }

        if (operationMap.containsKey(TaggingShapeUtils.TAG_RESOURCE_OPNAME)) {
            ShapeId tagOperationId = operationMap.get(TaggingShapeUtils.TAG_RESOURCE_OPNAME);
            shapeToTagOperation.put(service.getId(), tagOperationId);
            serviceResources.forEach(resourceId -> shapeToTagOperation.put(resourceId, tagOperationId));
            OperationShape tagOperation = model.expectShape(tagOperationId, OperationShape.class);
            hasTagApi = TaggingShapeUtils.verifyTagResourceOperation(model, service, tagOperation, operationIndex);
            if (hasTagApi) {
                serviceTagOperationIsValid.add(service.getId());
            }
        }

        if (operationMap.containsKey(TaggingShapeUtils.UNTAG_RESOURCE_OPNAME)) {
            ShapeId untagOperationId = operationMap.get(TaggingShapeUtils.UNTAG_RESOURCE_OPNAME);
            shapeToUntagOperation.put(service.getId(), untagOperationId);
            serviceResources.forEach(resourceId -> shapeToUntagOperation.put(resourceId, untagOperationId));
            OperationShape untagOperation = model.expectShape(untagOperationId, OperationShape.class);
            hasUntagApi = TaggingShapeUtils.verifyUntagResourceOperation(model, service, untagOperation,
                                                    operationIndex);
            if (hasUntagApi) {
                serviceUntagOperationIsValid.add(service.getId());
            }
        }

        if (operationMap.containsKey(TaggingShapeUtils.LIST_TAGS_OPNAME)) {
            ShapeId listTagsOperationId = operationMap.get(TaggingShapeUtils.LIST_TAGS_OPNAME);
            shapeToListTagsOperation.put(service.getId(), listTagsOperationId);
            serviceResources.forEach(resourceId -> shapeToListTagsOperation.put(resourceId, listTagsOperationId));
            OperationShape listTagsOperation = model.expectShape(listTagsOperationId, OperationShape.class);
            hasListTagsApi = TaggingShapeUtils.verifyListTagsOperation(model, service, listTagsOperation,
                                                    operationIndex);
            if (hasListTagsApi) {
                serviceListTagsOperationIsValid.add(service.getId());
            }
        }

        return hasTagApi && hasUntagApi && hasListTagsApi;
    }

    /**
     * Returns the input member that has a name which matches for tags on a TagResource operation.
     *
     * Note: This is more of a utility method needed to expose limited access to an internal method.
     *
     * @param model Smithy model to follow member references from the provided operation shape.
     * @param tagResourceOperation TagResource operation object to scan the input members of.
     * @return the matching input member if present. {@see java.util.Optional.empty()} otherwise.
     */
    @SmithyInternalApi
    public static Optional<MemberShape> getTagsMember(Model model, OperationShape tagResourceOperation) {
        for (MemberShape memberShape : model.expectShape(tagResourceOperation.getInputShape(),
                StructureShape.class).members()) {
            if (TaggingShapeUtils.isTagDesiredName(memberShape.getMemberName())) {
                return Optional.of(memberShape);
            }
        }
        return Optional.empty();
    }
}

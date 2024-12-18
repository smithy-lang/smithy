/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.NoReplaceTrait;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Index of AWS tagging trait information in a service closure and convenient
 * access to tag operations by name in service closures.
 */
@SmithyUnstableApi
public final class AwsTagIndex implements KnowledgeIndex {
    private final Map<ShapeId, MemberShape> operationTagMembers = new HashMap<>();
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
        // Compute a service's tagging operations.
        for (ServiceShape service : model.getServiceShapesWithTrait(TagEnabledTrait.class)) {
            computeTaggingApis(model, service);
            if (serviceTagOperationIsValid.contains(service.getId())
                    && serviceUntagOperationIsValid.contains(service.getId())
                    && serviceListTagsOperationIsValid.contains(service.getId())) {
                servicesWithAllTagOperations.add(service.getId());
            }
        }

        // Compute if a resource meets tag-on-create or tag-on-update criteria.
        for (ResourceShape resource : model.getResourceShapesWithTrait(TaggableTrait.class)) {
            if (!resource.expectTrait(TaggableTrait.class).getProperty().isPresent()) {
                continue;
            }

            computeResourceTagLocations(model, resource);
        }

        // Compute any operation's tag input member.
        OperationIndex operationIndex = OperationIndex.of(model);
        for (OperationShape operation : model.getOperationShapes()) {
            for (MemberShape memberShape : operationIndex.getInputMembers(operation).values()) {
                if (TaggingShapeUtils.isTagDesiredName(memberShape.getMemberName())) {
                    operationTagMembers.put(operation.getId(), memberShape);
                }
            }
        }
    }

    public static AwsTagIndex of(Model model) {
        return model.getKnowledge(AwsTagIndex.class, AwsTagIndex::new);
    }

    /**
     * Returns the input member that has a name which matches for tags on a TagResource operation.
     *
     * @param operation operation object to scan the input members of.
     * @return an Optional containing the matching input member if present, otherwise an empty Optional.
     */
    public Optional<MemberShape> getTagsMember(ToShapeId operation) {
        return Optional.ofNullable(operationTagMembers.get(operation.toShapeId()));
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
    public boolean isResourceTagOnCreate(ToShapeId resourceId) {
        return resourceIsTagOnCreate.contains(resourceId.toShapeId());
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
    public boolean isResourceTagOnUpdate(ToShapeId resourceId) {
        return resourceIsTagOnUpdate.contains(resourceId.toShapeId());
    }

    /**
     * Checks if a given ShapeID references a service shape that meets the criteria for having the three
     * expected service-wide tagging APIs.
     *
     * @param serviceShapeId ShapeID of the shape to check.
     * @return true iff the serviceShapeId references a service shape that has the necessary tagging APIs.
     */
    public boolean serviceHasTagApis(ToShapeId serviceShapeId) {
        return servicesWithAllTagOperations.contains(serviceShapeId.toShapeId());
    }

    /**
     * Gets the ShapeID of the TagResource operation on the shape if one is found by name.
     * If a resource ID is passed in, it will return the qualifiying service wide TagResource operation ID rather than
     * the operation ID specified by the tagApi property.
     *
     * @param serviceOrResourceId ShapeID of the service shape to retrieve the qualifying TagResource operation for.
     * @return The ShapeID of a qualifying TagResource operation if one is found. Returns an empty optional otherwise.
     */
    public Optional<ShapeId> getTagResourceOperation(ToShapeId serviceOrResourceId) {
        return Optional.ofNullable(shapeToTagOperation.get(serviceOrResourceId.toShapeId()));
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
    public Optional<ShapeId> getUntagResourceOperation(ToShapeId serviceOrResourceId) {
        return Optional.ofNullable(shapeToUntagOperation.get(serviceOrResourceId.toShapeId()));
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
    public Optional<ShapeId> getListTagsForResourceOperation(ToShapeId serviceOrResourceId) {
        return Optional.ofNullable(shapeToListTagsOperation.get(serviceOrResourceId.toShapeId()));
    }

    /**
     * Gets the verification result of whether or not the service has a named TagResource operation that meets the
     * expected critiera for TagResource.
     *
     * @param serviceId the ShapeID of the service shape the TagResource operation should be bound to.
     * @return True iff the service shape has a service bound operation named 'TagResource' that also satisfies
     *   the criteria for a valid TagResource operation.
     */
    public boolean serviceHasValidTagResourceOperation(ToShapeId serviceId) {
        return serviceTagOperationIsValid.contains(serviceId.toShapeId());
    }

    /**
     * Gets the verification result of whether or not the service has a named UntagResource operation that meets the
     * expected critiera for UntagResource.
     *
     * @param serviceId the ShapeID of the service shape the UntagResource operation should be bound to.
     * @return True iff the service shape has a service bound operation named 'UntagResource' that also satisfies
     *   the criteria for a valid UntagResource operation.
     */
    public boolean serviceHasValidUntagResourceOperation(ToShapeId serviceId) {
        return serviceUntagOperationIsValid.contains(serviceId.toShapeId());
    }

    /**
     * Gets the verification result of whether or not the service has a named ListTagsForResource operation that meets
     * the expected critiera for ListTagsForResource.
     *
     * @param serviceId the ShapeID of the service shape the ListTagsForResource operation should be bound to.
     * @return True iff the service shape has a service bound operation named 'ListTagsForResource' that also satisfies
     *   the criteria for a valid ListTagsForResource operation.
     */
    public boolean serviceHasValidListTagsForResourceOperation(ToShapeId serviceId) {
        return serviceListTagsOperationIsValid.contains(serviceId.toShapeId());
    }

    private void computeTaggingApis(Model model, ServiceShape service) {
        Map<String, ShapeId> operationMap = new HashMap<>();
        for (ShapeId operationId : service.getOperations()) {
            operationMap.put(operationId.getName(), operationId);
        }

        calculateTagApi(model, service, operationMap);
        calculateUntagApi(model, service, operationMap);
        calculateListTagsApi(model, service, operationMap);
    }

    private void calculateTagApi(
            Model model,
            ServiceShape service,
            Map<String, ShapeId> operationMap
    ) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        OperationIndex operationIndex = OperationIndex.of(model);

        if (operationMap.containsKey(TaggingShapeUtils.TAG_RESOURCE_OPNAME)) {
            ShapeId tagOperationId = operationMap.get(TaggingShapeUtils.TAG_RESOURCE_OPNAME);
            shapeToTagOperation.put(service.getId(), tagOperationId);
            OperationShape tagOperation = model.expectShape(tagOperationId, OperationShape.class);
            if (TaggingShapeUtils.verifyTagResourceOperation(model, tagOperation, operationIndex)) {
                serviceTagOperationIsValid.add(service.getId());
            }
        }
        for (ResourceShape resourceShape : topDownIndex.getContainedResources(service)) {
            shapeToTagOperation.put(resourceShape.getId(),
                    resourceShape.getTrait(TaggableTrait.class)
                            .flatMap(TaggableTrait::getApiConfig)
                            .map(TaggableApiConfig::getTagApi)
                            .orElse(shapeToTagOperation.get(service.getId())));
        }
    }

    private void calculateUntagApi(
            Model model,
            ServiceShape service,
            Map<String, ShapeId> operationMap
    ) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        OperationIndex operationIndex = OperationIndex.of(model);

        if (operationMap.containsKey(TaggingShapeUtils.UNTAG_RESOURCE_OPNAME)) {
            ShapeId untagOperationId = operationMap.get(TaggingShapeUtils.UNTAG_RESOURCE_OPNAME);
            shapeToUntagOperation.put(service.getId(), untagOperationId);
            OperationShape untagOperation = model.expectShape(untagOperationId, OperationShape.class);
            if (TaggingShapeUtils.verifyUntagResourceOperation(model, untagOperation, operationIndex)) {
                serviceUntagOperationIsValid.add(service.getId());
            }
        }
        for (ResourceShape resourceShape : topDownIndex.getContainedResources(service)) {
            shapeToUntagOperation.put(resourceShape.getId(),
                    resourceShape.getTrait(TaggableTrait.class)
                            .flatMap(TaggableTrait::getApiConfig)
                            .map(TaggableApiConfig::getUntagApi)
                            .orElse(shapeToUntagOperation.get(service.getId())));
        }
    }

    private void calculateListTagsApi(
            Model model,
            ServiceShape service,
            Map<String, ShapeId> operationMap
    ) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        OperationIndex operationIndex = OperationIndex.of(model);

        if (operationMap.containsKey(TaggingShapeUtils.LIST_TAGS_OPNAME)) {
            ShapeId listTagsOperationId = operationMap.get(TaggingShapeUtils.LIST_TAGS_OPNAME);
            shapeToListTagsOperation.put(service.getId(), listTagsOperationId);
            OperationShape listTagsOperation = model.expectShape(listTagsOperationId, OperationShape.class);
            if (TaggingShapeUtils.verifyListTagsOperation(model, listTagsOperation, operationIndex)) {
                serviceListTagsOperationIsValid.add(service.getId());
            }
        }
        for (ResourceShape resourceShape : topDownIndex.getContainedResources(service)) {
            shapeToListTagsOperation.put(resourceShape.getId(),
                    resourceShape.getTrait(TaggableTrait.class)
                            .flatMap(TaggableTrait::getApiConfig)
                            .map(TaggableApiConfig::getListTagsApi)
                            .orElse(shapeToListTagsOperation.get(service.getId())));
        }
    }

    private void computeResourceTagLocations(Model model, ResourceShape resource) {
        // Check if tag property is specified on create.
        if (TaggingShapeUtils.isTagPropertyInInput(resource.getCreate(), model, resource)) {
            resourceIsTagOnCreate.add(resource.getId());
        }
        // Check if tag property is specified on update.
        if (TaggingShapeUtils.isTagPropertyInInput(resource.getUpdate(), model, resource)) {
            resourceIsTagOnUpdate.add(resource.getId());
        }

        // Check if tag property is specified on put.
        if (TaggingShapeUtils.isTagPropertyInInput(resource.getPut(), model, resource)) {
            // Put is always a create operation
            resourceIsTagOnCreate.add(resource.getId());
            // and it's also an update when not annotated with `@noReplace`.
            if (!resource.hasTrait(NoReplaceTrait.class)) {
                resourceIsTagOnUpdate.add(resource.getId());
            }
        }
    }
}

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

import java.util.HashSet;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.PropertyBindingIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Index of AWS tagging trait information in a service closure.
 */
public final class AwsTagIndex implements KnowledgeIndex {
    private final Set<ShapeId> servicesWithAllTagOperations = new HashSet<>();
    private final Set<ShapeId> resourceIsTagOnCreate = new HashSet<>();
    private final Set<ShapeId> resourceIsTagOnUpdate = new HashSet<>();

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

    private boolean verifyTagApis(Model model, ServiceShape service, OperationIndex operationIndex) {
        boolean hasTagApi = false;
        boolean hasUntagApi = false;
        boolean hasListTagsApi = false;

        hasTagApi = TaggingShapeUtils.verifyTagResourceOperation(model, service, operationIndex);

        hasUntagApi = TaggingShapeUtils.verifyUntagResourceOperation(model, service, operationIndex);

        hasListTagsApi = TaggingShapeUtils.verifyListTagsOperation(model, service, operationIndex);

        return hasTagApi && hasUntagApi && hasListTagsApi;
    }

    public static AwsTagIndex of(Model model) {
        return new AwsTagIndex(model);
    }

    public boolean isResourceTagOnCreate(ShapeId resourceId) {
        return resourceIsTagOnCreate.contains(resourceId);
    }

    public boolean isResourceTagOnUpdate(ShapeId resourceId) {
        return resourceIsTagOnUpdate.contains(resourceId);
    }

    public boolean serviceHasTagApis(ShapeId serviceShapeId) {
        return servicesWithAllTagOperations.contains(serviceShapeId);
    }
}

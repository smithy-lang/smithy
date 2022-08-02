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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import software.amazon.smithy.aws.traits.ArnTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.PropertyBindingIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that service satisfies the AWS tagging requirements.
 */
public final class TaggableResourceValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new LinkedList<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        AwsTagIndex tagIndex = AwsTagIndex.of(model);
        PropertyBindingIndex propertyBindingIndex = PropertyBindingIndex.of(model);
        for (ServiceShape service : model.getServiceShapesWithTrait(TagEnabledTrait.class)) {
            for (ResourceShape resource : topDownIndex.getContainedResources(service)) {
                if (resource.hasTrait(TaggableTrait.class)) {
                    events.addAll(validateResource(model, resource, service, tagIndex, propertyBindingIndex));
                } else if (resource.hasTrait(ArnTrait.class) && tagIndex.serviceHasTagApis(service.getId())) {
                    // If a resource does not have the taggable trait, but has an ARN, and the service has tag
                    // operations, it is most likely a mistake.
                    events.add(warning(resource, "Resource is likely missing `aws.api#taggable` trait."));
                }
            }
        }
        return events;
    }

    private List<ValidationEvent> validateResource(
            Model model,
            ResourceShape resource,
            ServiceShape service,
            AwsTagIndex awsTagIndex,
            PropertyBindingIndex propertyBindingIndex
    ) {
        List<ValidationEvent> events = new LinkedList<>();
        // Generate danger if resource has tag property in update API.
        if (awsTagIndex.isResourceTagOnUpdate(resource.getId())) {
            Shape updateOperation = model.expectShape(resource.getUpdate().get());
            events.add(danger(updateOperation, "Update resource lifecycle operation should not support updating tags"
                    + " because it is a privileged operation that modifies access."));
        }
        // A valid taggable resource must support one of the following:
        // 1. Tagging via service-wide TagResource/UntagResource/ListTagsForResource
        //    APIs that have input which targets a resource ARN and have expected tag
        //    list or tag keys input or output member
        // 2. Tagging via APIs specified in the @taggable trait which are validated
        //    through the tag property, and must be resource instance operations
        //Caution: avoid short circuiting behavior.
        boolean isServiceWideTaggable = awsTagIndex.serviceHasTagApis(service.getId());
        boolean isInstanceOpTaggable = isTaggableViaInstanceOperations(events, model, resource, service,
                propertyBindingIndex);

        if (isServiceWideTaggable && !isInstanceOpTaggable && !resource.hasTrait(ArnTrait.class)) {
            events.add(error(resource, "Resource is taggable only via service-wide tag operations."
                    + " It must use the `aws.api@arn` trait."));
        }

        if (!(isServiceWideTaggable || isInstanceOpTaggable)) {
            events.add(error(resource, "Resource does not have tagging CRUD operations and is not compatible"
                    + " with service-wide tagging operations."));
        }

        return events;
    }

    private Optional<OperationShape> resolveTagOperation(ShapeId tagApiId, Model model) {
        return model.getShape(tagApiId).flatMap(shape -> shape.asOperationShape());
    }

    private boolean isTaggableViaInstanceOperations(
            List<ValidationEvent> events,
            Model model,
            ResourceShape resource,
            ServiceShape service,
            PropertyBindingIndex propertyBindingIndex
    ) {
        TaggableTrait taggableTrait = resource.expectTrait(TaggableTrait.class);
        if (taggableTrait.getApiConfig().isPresent()) {
            TaggableApiConfig apiConfig = taggableTrait.getApiConfig().get();
            boolean tagApiVerified = false;
            boolean untagApiVerified = false;
            boolean listTagsApiVerified = false;

            Optional<OperationShape> tagApi = resolveTagOperation(apiConfig.getTagApi(), model);
            if (tagApi.isPresent()) {
                tagApiVerified = TaggingShapeUtils.isTagPropertyInInput(Optional.of(
                        tagApi.get().getId()), model, resource, propertyBindingIndex)
                        && verifyTagApi(tagApi.get(), model, service, resource);
            }

            Optional<OperationShape> untagApi = resolveTagOperation(apiConfig.getUntagApi(), model);
            if (untagApi.isPresent()) {
                untagApiVerified = verifyUntagApi(untagApi.get(), model, service, resource);
            }


            Optional<OperationShape> listTagsApi = resolveTagOperation(apiConfig.getListTagsApi(), model);
            if (listTagsApi.isPresent()) {
                listTagsApiVerified = verifyListTagsApi(listTagsApi.get(), model, service, resource);
            }

            return tagApiVerified && untagApiVerified && listTagsApiVerified;
        }
        return false;
    }

    private boolean verifyListTagsApi(
        OperationShape listTagsApi,
        Model model,
        ServiceShape service,
        ResourceShape resource
    ) {
        // Verify Tags map or list member but on the output.
        return exactlyOne(collectMemberTargetShapes(listTagsApi.getOutputShape(), model),
                memberEntry -> TaggingShapeUtils.isTagDesiredName(memberEntry.getKey().getMemberName())
                && TaggingShapeUtils.verifyTagsShape(model, memberEntry.getValue()));
    }

    private boolean verifyUntagApi(
        OperationShape untagApi,
        Model model,
        ServiceShape service,
        ResourceShape resource
    ) {
        // Tag API has a tags property on its input AND has exactly one member targetting a tag shape with an
        // appropriate name.
        return exactlyOne(collectMemberTargetShapes(untagApi.getInputShape(), model),
                memberEntry -> TaggingShapeUtils.isTagKeysDesiredName(memberEntry.getKey().getMemberName())
                && TaggingShapeUtils.verifyTagKeysShape(model, memberEntry.getValue()));
    }

    private boolean verifyTagApi(
        OperationShape tagApi,
        Model model,
        ServiceShape service,
        ResourceShape resource
    ) {
        // Tag API has exactly one member targetting a tag list or map shape with an appropriate name.
        return exactlyOne(collectMemberTargetShapes(tagApi.getInputShape(), model),
                memberEntry -> TaggingShapeUtils.isTagDesiredName(memberEntry.getKey().getMemberName())
                && TaggingShapeUtils.verifyTagsShape(model, memberEntry.getValue()));
    }

    private boolean exactlyOne(
        Collection<Map.Entry<MemberShape, Shape>> collection,
        Predicate<Map.Entry<MemberShape, Shape>> test
    ) {
        int count = 0;
        for (Map.Entry<MemberShape, Shape> entry : collection) {
            if (test.test(entry)) {
                ++count;
            }
        }
        return count == 1;
    }

    private Collection<Map.Entry<MemberShape, Shape>> collectMemberTargetShapes(ShapeId ioShapeId, Model model) {
        Collection<Map.Entry<MemberShape, Shape>> collection = new LinkedList<>();
        for (MemberShape memberShape : model.expectShape(ioShapeId).members()) {
            collection.add(new AbstractMap.SimpleImmutableEntry<>(
                    memberShape, model.expectShape(memberShape.getTarget())));
        }
        return collection;
    }
}

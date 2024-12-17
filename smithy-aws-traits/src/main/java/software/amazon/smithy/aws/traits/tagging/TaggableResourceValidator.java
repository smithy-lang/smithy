/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import software.amazon.smithy.aws.traits.ArnTrait;
import software.amazon.smithy.model.Model;
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
        List<ValidationEvent> events = new ArrayList<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        AwsTagIndex tagIndex = AwsTagIndex.of(model);
        for (ServiceShape service : model.getServiceShapes()) {
            for (ResourceShape resource : topDownIndex.getContainedResources(service)) {
                boolean resourceLikelyTaggable = false;
                if (resource.hasTrait(TaggableTrait.class)) {
                    events.addAll(validateResource(model, resource, service, tagIndex));
                    resourceLikelyTaggable = true;
                } else if (resource.hasTrait(ArnTrait.class) && tagIndex.serviceHasTagApis(service)) {
                    // If a resource does not have the taggable trait, but has an ARN, and the service has tag
                    // operations, it is most likely a mistake.
                    events.add(warning(resource, "Resource is likely missing `aws.api#taggable` trait."));
                    resourceLikelyTaggable = true;
                }

                // It's possible the resource was marked as taggable but the service isn't tagEnabled.
                if (resourceLikelyTaggable && !service.hasTrait(TagEnabledTrait.class)) {
                    events.add(warning(service,
                            "Service has resources with `aws.api#taggable` applied but does not "
                                    + "have the `aws.api#tagEnabled` trait."));
                }
            }
        }
        return events;
    }

    private List<ValidationEvent> validateResource(
            Model model,
            ResourceShape resource,
            ServiceShape service,
            AwsTagIndex awsTagIndex
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        // Generate danger if resource has tag property in update API.
        if (awsTagIndex.isResourceTagOnUpdate(resource.getId())) {
            Shape operation = resource.getUpdate().isPresent()
                    ? model.expectShape(resource.getUpdate().get())
                    : model.expectShape(resource.getPut().get());
            events.add(danger(operation,
                    "Update and put resource lifecycle operations should not support updating tags"
                            + " because it is a privileged operation that modifies access."));
        }
        // A valid taggable resource must support one of the following:
        // 1. Tagging via service-wide TagResource/UntagResource/ListTagsForResource
        //    APIs that have input which targets a resource ARN and have expected tag
        //    list or tag keys input or output member
        // 2. Tagging via APIs specified in the @taggable trait which are validated
        //    through the tag property, and must be resource instance operations
        boolean isServiceWideTaggable = awsTagIndex.serviceHasTagApis(service.getId());
        boolean isInstanceOpTaggable = isTaggableViaInstanceOperations(model, resource);

        if (isServiceWideTaggable && !isInstanceOpTaggable && !resource.hasTrait(ArnTrait.class)) {
            events.add(error(resource,
                    "Resource is taggable only via service-wide tag operations."
                            + " It must use the `aws.api@arn` trait."));
        }

        if (!isServiceWideTaggable && !isInstanceOpTaggable) {
            events.add(error(resource,
                    String.format("Resource does not have tagging CRUD operations and is not"
                            + " compatible with service-wide tagging operations"
                            + " for service `%s`.",
                            service.getId())));
        }

        return events;
    }

    private Optional<OperationShape> resolveTagOperation(ShapeId tagApiId, Model model) {
        return model.getShape(tagApiId).flatMap(Shape::asOperationShape);
    }

    private boolean isTaggableViaInstanceOperations(Model model, ResourceShape resource) {
        TaggableTrait taggableTrait = resource.expectTrait(TaggableTrait.class);
        if (taggableTrait.getApiConfig().isPresent()) {
            TaggableApiConfig apiConfig = taggableTrait.getApiConfig().get();
            boolean tagApiVerified = false;
            boolean untagApiVerified = false;
            boolean listTagsApiVerified = false;

            Optional<OperationShape> tagApi = resolveTagOperation(apiConfig.getTagApi(), model);
            if (tagApi.isPresent()) {
                tagApiVerified = TaggingShapeUtils.isTagPropertyInInput(
                        Optional.of(tagApi.get().getId()),
                        model,
                        resource)
                        && verifyTagApi(tagApi.get(), model);
            }

            Optional<OperationShape> untagApi = resolveTagOperation(apiConfig.getUntagApi(), model);
            if (untagApi.isPresent()) {
                untagApiVerified = verifyUntagApi(untagApi.get(), model);
            }

            Optional<OperationShape> listTagsApi = resolveTagOperation(apiConfig.getListTagsApi(), model);
            if (listTagsApi.isPresent()) {
                listTagsApiVerified = verifyListTagsApi(listTagsApi.get(), model);
            }

            return tagApiVerified && untagApiVerified && listTagsApiVerified;
        }
        return false;
    }

    private boolean verifyListTagsApi(OperationShape listTagsApi, Model model) {
        // Verify Tags map or list member but on the output.
        return exactlyOne(collectMemberTargetShapes(listTagsApi.getOutputShape(), model),
                memberEntry -> TaggingShapeUtils.isTagDesiredName(memberEntry.getKey().getMemberName())
                        && TaggingShapeUtils.verifyTagsShape(model, memberEntry.getValue()));
    }

    private boolean verifyUntagApi(OperationShape untagApi, Model model) {
        // Tag API has a tags property on its input AND has exactly one member targeting a tag shape with an
        // appropriate name.
        return exactlyOne(collectMemberTargetShapes(untagApi.getInputShape(), model),
                memberEntry -> TaggingShapeUtils.isTagKeysDesiredName(memberEntry.getKey().getMemberName())
                        && TaggingShapeUtils.verifyTagKeysShape(model, memberEntry.getValue()));
    }

    private boolean verifyTagApi(OperationShape tagApi, Model model) {
        // Tag API has exactly one member targeting a tag list or map shape with an appropriate name.
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
        Collection<Map.Entry<MemberShape, Shape>> collection = new ArrayList<>();
        for (MemberShape memberShape : model.expectShape(ioShapeId).members()) {
            collection.add(new AbstractMap.SimpleImmutableEntry<>(
                    memberShape,
                    model.expectShape(memberShape.getTarget())));
        }
        return collection;
    }
}

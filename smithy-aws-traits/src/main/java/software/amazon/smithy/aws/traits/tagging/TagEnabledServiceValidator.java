/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates service has at least one taggable resource.
 */
public final class TagEnabledServiceValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        AwsTagIndex tagIndex = AwsTagIndex.of(model);
        for (ServiceShape service : model.getServiceShapesWithTrait(TagEnabledTrait.class)) {
            events.addAll(validateService(service, tagIndex, topDownIndex));
        }
        return events;
    }

    private List<ValidationEvent> validateService(
            ServiceShape service,
            AwsTagIndex tagIndex,
            TopDownIndex topDownIndex
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        TagEnabledTrait trait = service.expectTrait(TagEnabledTrait.class);

        int taggableResourceCount = 0;
        for (ResourceShape resource : topDownIndex.getContainedResources(service)) {
            if (resource.hasTrait(TaggableTrait.ID)) {
                ++taggableResourceCount;
            }
        }
        if (taggableResourceCount == 0) {
            events.add(error(service,
                    "Service marked `aws.api#tagEnabled` trait must have at least one "
                            + "`aws.api#taggable` resource."));
        }

        if (!trait.getDisableDefaultOperations() && !tagIndex.serviceHasTagApis(service.getId())) {
            events.add(warning(service,
                    "Service marked `aws.api#tagEnabled` trait does not have"
                            + " consistent tagging operations implemented: {TagResource, UntagResource, and"
                            + " ListTagsForResource}."));
        }

        trait.getApiConfig().ifPresent(cfg -> {
            Set<String> opNames = new HashSet<>();
            for (ShapeId opId : service.getOperations()) {
                opNames.add(opId.getName());
            }
            checkApiConfigConflict(events,
                    service,
                    trait,
                    cfg.getTagApi(),
                    opNames,
                    TaggingShapeUtils.TAG_RESOURCE_OPNAME,
                    "tagApi");
            checkApiConfigConflict(events,
                    service,
                    trait,
                    cfg.getUntagApi(),
                    opNames,
                    TaggingShapeUtils.UNTAG_RESOURCE_OPNAME,
                    "untagApi");
            checkApiConfigConflict(events,
                    service,
                    trait,
                    cfg.getListTagsApi(),
                    opNames,
                    TaggingShapeUtils.LIST_TAGS_OPNAME,
                    "listTagsApi");
        });

        return events;
    }

    private void checkApiConfigConflict(
            List<ValidationEvent> events,
            ServiceShape service,
            TagEnabledTrait trait,
            Optional<ShapeId> configured,
            Set<String> opNames,
            String defaultOpName,
            String apiConfigMember
    ) {
        if (!configured.isPresent()) {
            return;
        }
        if (configured.get().getName().equals(defaultOpName)) {
            return;
        }
        if (!opNames.contains(defaultOpName)) {
            return;
        }
        events.add(warning(service,
                trait,
                "Service has `apiConfig." + apiConfigMember + "` set to `" + configured.get()
                        + "` but also has a service-bound operation named `" + defaultOpName
                        + "`. The default-named operation will be ignored for tagging API discovery."
                        + " Remove either the `apiConfig` override or the `" + defaultOpName
                        + "` operation to avoid ambiguity."));
    }
}

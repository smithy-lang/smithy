/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
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
            if (resource.hasTrait(TaggableTrait.class)) {
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
        return events;
    }
}

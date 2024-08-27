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

import java.util.LinkedList;
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
        List<ValidationEvent> events = new LinkedList<>();
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
        List<ValidationEvent> events = new LinkedList<>();
        TagEnabledTrait trait = service.expectTrait(TagEnabledTrait.class);

        int taggableResourceCount = 0;
        for (ResourceShape resource : topDownIndex.getContainedResources(service)) {
            if (resource.hasTrait(TaggableTrait.class)) {
                ++taggableResourceCount;
            }
        }
        if (taggableResourceCount == 0) {
            events.add(error(service, "Service marked `aws.api#tagEnabled` trait must have at least one "
                                        + "`aws.api#taggable` resource."));
        }

        if (!trait.getDisableDefaultOperations() && !tagIndex.serviceHasTagApis(service.getId())) {
            events.add(warning(service, "Service marked `aws.api#tagEnabled` trait does not have"
                    + " consistent tagging operations implemented: {TagResource, UntagResource, and"
                    + " ListTagsForResource}."));
        }
        return events;
    }
}

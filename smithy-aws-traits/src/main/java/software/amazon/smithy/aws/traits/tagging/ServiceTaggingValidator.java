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

import static software.amazon.smithy.aws.traits.tagging.TaggingShapeUtils.LIST_TAGS_OPNAME;
import static software.amazon.smithy.aws.traits.tagging.TaggingShapeUtils.TAG_RESOURCE_OPNAME;
import static software.amazon.smithy.aws.traits.tagging.TaggingShapeUtils.UNTAG_RESOURCE_OPNAME;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates service satisfies AWS tagging requirements.
 */
public final class ServiceTaggingValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        AwsTagIndex awsTagIndex = AwsTagIndex.of(model);
        List<ValidationEvent> events = new LinkedList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(TagEnabledTrait.class)) {
            events.addAll(validateService(service, awsTagIndex));
        }
        return events;
    }

    private List<ValidationEvent> validateService(ServiceShape service, AwsTagIndex awsTagIndex) {
        List<ValidationEvent> events = new LinkedList<>();
        TagEnabledTrait trait = service.expectTrait(TagEnabledTrait.class);

        Optional<ShapeId> tagResourceId = awsTagIndex.getTagResourceOperation(service.getId());
        if (tagResourceId.isPresent()) {
            if (!awsTagIndex.serviceHasValidTagResourceOperation(service.getId())) {
                events.add(getInvalidOperationEvent(service, trait, tagResourceId.get(), TAG_RESOURCE_OPNAME));
            }
        } else {
            events.add(getMissingOperationEvent(service, trait, TAG_RESOURCE_OPNAME));
        }

        Optional<ShapeId> untagResourceId = awsTagIndex.getUntagResourceOperation(service.getId());
        if (untagResourceId.isPresent()) {
            if (!awsTagIndex.serviceHasValidUntagResourceOperation(service.getId())) {
                events.add(getInvalidOperationEvent(service, trait, untagResourceId.get(), UNTAG_RESOURCE_OPNAME));
            }
        } else {
            events.add(getMissingOperationEvent(service, trait, UNTAG_RESOURCE_OPNAME));
        }

        Optional<ShapeId> listTagsId = awsTagIndex.getListTagsForResourceOperation(service.getId());
        if (listTagsId.isPresent()) {
            if (!awsTagIndex.serviceHasValidListTagsForResourceOperation(service.getId())) {
                events.add(getInvalidOperationEvent(service, trait, listTagsId.get(), LIST_TAGS_OPNAME));
            }
        } else {
            events.add(getMissingOperationEvent(service, trait, LIST_TAGS_OPNAME));
        }

        return events;
    }

    private ValidationEvent getMissingOperationEvent(ServiceShape service, FromSourceLocation location, String opName) {
        return warning(service, location, "Service marked `aws.api#TagEnabled` is missing an operation named "
                + "'" + opName + ".'");
    }

    private ValidationEvent getInvalidOperationEvent(
        ServiceShape service,
        FromSourceLocation location,
        ShapeId opId,
        String opName
    ) {
        return danger(service, location, String.format("Shape `%s` does not satisfy '%s' operation requirements.",
                        opId.toString(), opName));
    }
}

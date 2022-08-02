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
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates service satisfies AWS tagging requirements.
 */
public final class ServiceTaggingValidator extends AbstractValidator {
    private static final String TAG_RESOURCE_OPNAME = "TagResource";
    private static final String UNTAG_RESOURCE_OPNAME = "UntagResource";
    private static final String LISTTAGS_OPNAME = "ListTagsForResource";

    @Override
    public List<ValidationEvent> validate(Model model) {
        AwsTagIndex awsTagIndex = AwsTagIndex.of(model);
        List<ValidationEvent> events = new LinkedList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(TagEnabledTrait.class)) {
            events.addAll(validateService(model, service, awsTagIndex));
        }
        return events;
    }

    private List<ValidationEvent> validateService(Model model, ServiceShape service, AwsTagIndex awsTagIndex) {
        List<ValidationEvent> events = new LinkedList<>();
        SourceLocation tagEnabledTraitLoc = service.expectTrait(TagEnabledTrait.class).getSourceLocation();

        Optional<ShapeId> tagResourceId = awsTagIndex.getTagResourceOperation(service.getId());
        if (tagResourceId.isPresent()) {
            if (!awsTagIndex.serviceHasValidTagResourceOperation(service.getId())) {
                events.add(getMessageUnqualifedOperation(service, tagEnabledTraitLoc,
                                                tagResourceId.get(), TAG_RESOURCE_OPNAME));
            }
        } else {
            events.add(getMessageMissingOperation(service, tagEnabledTraitLoc, TAG_RESOURCE_OPNAME));
        }

        Optional<ShapeId> untagResourceId = awsTagIndex.getUntagResourceOperation(service.getId());
        if (untagResourceId.isPresent()) {
            if (!awsTagIndex.serviceHasValidUntagResourceOperation(service.getId())) {
                events.add(getMessageUnqualifedOperation(service, tagEnabledTraitLoc,
                                                untagResourceId.get(), UNTAG_RESOURCE_OPNAME));
            }
        } else {
            events.add(getMessageMissingOperation(service, tagEnabledTraitLoc, UNTAG_RESOURCE_OPNAME));
        }

        Optional<ShapeId> listTagResourceId = awsTagIndex.getListTagsForResourceOperation(service.getId());
        if (listTagResourceId.isPresent()) {
            if (!awsTagIndex.serviceHasValidListTagsForResourceOperation(service.getId())) {
                events.add(getMessageUnqualifedOperation(service, tagEnabledTraitLoc,
                                                listTagResourceId.get(), LISTTAGS_OPNAME));
            }
        } else {
            events.add(getMessageMissingOperation(service, tagEnabledTraitLoc, LISTTAGS_OPNAME));
        }

        return events;
    }

    private ValidationEvent getMessageMissingOperation(
        ServiceShape service,
        SourceLocation location,
        String opName
    ) {
        return warning(service, location, "Service marked `aws.api#TagEnabled` is missing an operation named "
                                            + "'" + opName + ".'");
    }

    private ValidationEvent getMessageUnqualifedOperation(
        ServiceShape service,
        SourceLocation location,
        ShapeId opId,
        String opName
    ) {
        return danger(service, location, String.format("Shape `%s` does not satisfy '%s' operation requirements.",
                        opId.toString(), opName));
    }
}

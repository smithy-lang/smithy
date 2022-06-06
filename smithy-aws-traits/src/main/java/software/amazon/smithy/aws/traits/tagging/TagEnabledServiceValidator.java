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
        for (ServiceShape service : model.getServiceShapesWithTrait(TagEnabledTrait.class)) {
            events.addAll(validateService(model, service));
        }
        return events;
    }

    private List<ValidationEvent> validateService(Model model, ServiceShape service) {
        List<ValidationEvent> events = new LinkedList<>();

        // Service must at least have one aws.api#taggable resource in its closure.
        if (!service.getResources().stream().map(resourceId -> model.expectShape(resourceId).asResourceShape().get())
                .filter(resource -> resource.hasTrait(TaggableTrait.class)).findAny().isPresent()) {
            events.add(error(service, "Service shape annotated with `aws.api#TagEnabled` trait must have at least one "
                                        + "`aws.api#taggable` resource."));
        }

        return events;
    }
}

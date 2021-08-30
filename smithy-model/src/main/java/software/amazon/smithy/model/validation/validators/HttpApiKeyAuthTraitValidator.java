/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that if an HttpApiKeyAuth trait's scheme field is present then
 * the 'in' field must specify "header". Scheme should only be used with the
 * "Authorization" http header.
 */
public final class HttpApiKeyAuthTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        Set<ServiceShape> serviceShapesWithTrait = model.getServiceShapesWithTrait(HttpApiKeyAuthTrait.class);
        List<ValidationEvent> events = new ArrayList<>();

        for (ServiceShape serviceShape : serviceShapesWithTrait) {
            HttpApiKeyAuthTrait trait = serviceShape.expectTrait(HttpApiKeyAuthTrait.class);
            trait.getScheme().ifPresent(scheme -> {
                if (trait.getIn() != HttpApiKeyAuthTrait.Location.HEADER) {
                    events.add(error(serviceShape, trait,
                            String.format("The httpApiKeyAuth trait must have an `in` value of `header` when a `scheme`"
                                    + " is provided, found: %s", trait.getIn())));
                }
            });
        }

        return events;
    }
}

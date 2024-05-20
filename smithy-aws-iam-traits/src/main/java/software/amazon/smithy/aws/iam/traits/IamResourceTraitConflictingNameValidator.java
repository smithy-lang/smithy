/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.iam.traits;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.ArnTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Ensures that there is no resource name conflict in a service closure
 * in IAM space after processing {@link IamResourceTrait}.
 */
@SmithyInternalApi
public class IamResourceTraitConflictingNameValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        return model.shapes(ServiceShape.class)
                .flatMap(shape -> validateService(topDownIndex, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(TopDownIndex topDownIndex, ServiceShape service) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<String> container = new HashSet<>();
        for (ResourceShape resource : topDownIndex.getContainedResources(service)) {
            String resourceName = resource.getId().getName();
            IamResourceTrait iamResourceTrait;
            if (resource.hasTrait(IamResourceTrait.class) && resource.getTrait(IamResourceTrait.class).isPresent()
                    && resource.getTrait(IamResourceTrait.class).get().getName().isPresent()) {
                resourceName = resource.getTrait(IamResourceTrait.class).get().getName().get();
            }
            if (container.contains(resourceName)) {
                events.add(error(resource, String.format(
                        "Conflicting IAM resource names in an entire service closure is not allowed. "
                                + "This IAM resource name name conflicts with other resource in the service `%s`.",
                        service.getId())));

            }
            container.add(resourceName);
        }
        return events;
    }
}

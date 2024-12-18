/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * Checks properties of the defineConditionKeys IAM trait
 * 1. @required only for service resolved condition keys. ERROR if used for request resolved condition keys.
 */
public final class DefineConditionKeysTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        ConditionKeysIndex conditionKeysIndex = ConditionKeysIndex.of(model);
        for (ServiceShape serviceShape : model.getServiceShapes()) {
            for (Map.Entry<String, ConditionKeyDefinition> conditionKeyEntry : conditionKeysIndex
                    .getDefinedConditionKeys(serviceShape)
                    .entrySet()) {
                // Get all the service resolved condition keys.
                List<String> serviceResolvedKeys = serviceShape.getTrait(ServiceResolvedConditionKeysTrait.class)
                        .map(StringListTrait::getValues)
                        .orElse(ListUtils.of());
                // We want to emit an event for definitions that state being required...
                if (conditionKeyEntry.getValue().isRequired()
                        // and are not configured to be service resolved.
                        && !serviceResolvedKeys.contains(conditionKeyEntry.getKey())) {
                    FromSourceLocation sourceLocation = serviceShape.getTrait(DefineConditionKeysTrait.class)
                            .map(Trait::getSourceLocation)
                            .orElse(serviceShape.getSourceLocation());
                    events.add(error(serviceShape,
                            sourceLocation,
                            format("The `%s` condition key is defined as "
                                    + "required but is resolved from the request. This property is only valid for condition "
                                    + "keys resolved by the service, it MUST also be specified in the "
                                    + "`@serviceResolvedConditionKeys` trait or use the `@required` trait instead.",
                                    conditionKeyEntry.getKey())));
                }
            }
        }
        return events;
    }
}

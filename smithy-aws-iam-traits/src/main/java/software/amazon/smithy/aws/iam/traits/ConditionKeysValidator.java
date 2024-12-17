/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Ensures that condition keys referenced by operations bound within the
 * closure of a service are defined either explicitly using the
 * {@code defineConditionKeys} trait or through an inferred resource
 * identifier condition key.
 *
 * <p>Condition keys that refer to global "aws:*" keys are allowed to not
 * be defined on the service.
 */
@SmithyInternalApi
public final class ConditionKeysValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ConditionKeysIndex conditionIndex = ConditionKeysIndex.of(model);
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        OperationIndex operationIndex = OperationIndex.of(model);

        return model.shapes(ServiceShape.class)
                .filter(service -> service.hasTrait(ServiceTrait.class))
                .flatMap(service -> {
                    List<ValidationEvent> results = new ArrayList<>();
                    Set<String> knownKeys = conditionIndex.getDefinedConditionKeys(service).keySet();
                    Set<String> serviceResolvedKeys = Collections.emptySet();

                    if (service.hasTrait(ServiceResolvedConditionKeysTrait.class)) {
                        ServiceResolvedConditionKeysTrait trait =
                                service.expectTrait(ServiceResolvedConditionKeysTrait.class);
                        //assign so we can compare against condition key values for any intersection
                        serviceResolvedKeys = new HashSet<>(trait.getValues());
                        //copy as this is a destructive action and will affect all future access
                        List<String> invalidNames = new ArrayList<>(trait.getValues());
                        invalidNames.removeAll(knownKeys);
                        if (!invalidNames.isEmpty()) {
                            results.add(error(service,
                                    trait,
                                    String.format(
                                            "This condition keys resolved by the `%s` service "
                                                    + "refer to undefined "
                                                    + "condition key(s) [%s]. Expected one of the following "
                                                    + "defined condition keys: [%s]",
                                            service.getId(),
                                            ValidationUtils.tickedList(invalidNames),
                                            ValidationUtils.tickedList(knownKeys))));
                        }
                    }

                    for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
                        for (String name : conditionIndex.getConditionKeyNames(service, operation)) {
                            if (!knownKeys.contains(name) && !name.startsWith("aws:")) {
                                results.add(error(operation,
                                        String.format(
                                                "This operation scoped within the `%s` service refers to an undefined "
                                                        + "condition key `%s`. Expected one of the following defined condition "
                                                        + "keys: [%s]",
                                                service.getId(),
                                                name,
                                                ValidationUtils.tickedList(knownKeys))));
                            }
                        }

                        for (MemberShape memberShape : operationIndex.getInputMembers(operation).values()) {
                            if (memberShape.hasTrait(ConditionKeyValueTrait.class)) {
                                ConditionKeyValueTrait trait = memberShape.expectTrait(ConditionKeyValueTrait.class);
                                String conditionKey = trait.getValue();
                                if (!knownKeys.contains(conditionKey)) {
                                    results.add(error(memberShape,
                                            trait,
                                            String.format(
                                                    "This operation `%s` scoped within the `%s` service with member `%s` "
                                                            + "refers to an undefined "
                                                            + "condition key `%s`. Expected one of the following defined "
                                                            + "condition keys: [%s]",
                                                    operation.getId(),
                                                    service.getId(),
                                                    memberShape.getId(),
                                                    conditionKey,
                                                    ValidationUtils.tickedList(knownKeys))));
                                }
                                if (serviceResolvedKeys.contains(conditionKey)) {
                                    results.add(error(memberShape,
                                            trait,
                                            String.format(
                                                    "This operation `%s` scoped within the `%s` service with member `%s` refers"
                                                            + " to a condition key `%s` that is also resolved by service.",
                                                    operation.getId(),
                                                    service.getId(),
                                                    memberShape.getId(),
                                                    conditionKey,
                                                    ValidationUtils.tickedList(knownKeys))));
                                }
                            }
                        }
                    }

                    return results.stream();
                })
                .collect(Collectors.toList());
    }
}

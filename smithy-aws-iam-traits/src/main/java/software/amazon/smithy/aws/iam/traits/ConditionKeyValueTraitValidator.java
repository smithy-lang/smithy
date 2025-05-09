/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
 * Validates that members match 1:1 with supplying a value for a specific condition key.
 */
public class ConditionKeyValueTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        if (!model.getAppliedTraits().contains(ConditionKeyValueTrait.ID)) {
            return events;
        }

        TopDownIndex topDownIndex = TopDownIndex.of(model);
        OperationIndex operationIndex = OperationIndex.of(model);
        for (ServiceShape service : model.getServiceShapesWithTrait(ServiceTrait.class)) {
            for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
                Map<String, List<String>> conditionKeyMembers = new HashMap<>();

                // Store each member name where the same condition key value is derived.
                for (MemberShape memberShape : operationIndex.getInputMembers(operation)
                        .values()) {
                    if (memberShape.hasTrait(ConditionKeyValueTrait.ID)) {
                        ConditionKeyValueTrait trait = memberShape.expectTrait(ConditionKeyValueTrait.class);
                        String conditionKey = trait.resolveConditionKey(service);
                        conditionKeyMembers.computeIfAbsent(conditionKey, k -> new ArrayList<>())
                                .add(memberShape.getMemberName());
                    }
                }

                // Emit events if more than one value source member is found for the same key.
                for (Map.Entry<String, List<String>> entry : conditionKeyMembers.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        events.add(error(operationIndex.expectInputShape(operation),
                                format("The `%s` condition key has its value supplied in the `%s` operation's input "
                                        + "shape by multiple members [%s]. This value must only be supplied by one "
                                        + "member.",
                                        entry.getKey(),
                                        operation.getId(),
                                        ValidationUtils.tickedList(entry.getValue()))));
                    }
                }
            }
        }
        return events;
    }
}

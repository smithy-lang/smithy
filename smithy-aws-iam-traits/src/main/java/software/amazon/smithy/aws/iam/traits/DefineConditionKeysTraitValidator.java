/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.aws.iam.traits;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;


/**
 * Checks properties of the defineConditionKeys IAM trait
 * 1. @required only for service resolved condition keys. ERROR if used for request resolved condition keys.
 */
public final class DefineConditionKeysTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        class Record {
            final Shape shape;
            final Entry<String, ConditionKeyDefinition> details;

            Record(Shape shape, Set<Entry<String, ConditionKeyDefinition>> details) {
                this.shape = shape;
                this.details = details.iterator().next();
            }
        }

        Set<Shape> defineConditionKeys = model.getShapesWithTrait(DefineConditionKeysTrait.class);
        Set<Shape> serviceResolvedConditionKeys = model.getShapesWithTrait(ServiceResolvedConditionKeysTrait.class);

        // get all the service resolved condition keys
        Set<String> serviceResolvedKeynames = serviceResolvedConditionKeys.stream().map(shape -> {
            ServiceResolvedConditionKeysTrait trait = shape.expectTrait(ServiceResolvedConditionKeysTrait.class);
            return trait.getValues();
        }).flatMap(Collection::stream).collect(Collectors.toSet());

        // get all the defined condition keys that are required
        Set<Record> requiredConditionKeyRecords = defineConditionKeys.stream().map(shape -> {
            DefineConditionKeysTrait trait = shape.expectTrait(DefineConditionKeysTrait.class);
            return new Record(shape, trait.getConditionKeys().entrySet());
        }).filter(record -> record.details.getValue().isRequired()).collect(Collectors.toSet());

        // get all the defined condition keys are required though not defined as service resolved condition keys
        Set<Record> incorretRequestResolved = requiredConditionKeyRecords.stream().filter(record ->
                !serviceResolvedKeynames.contains(record.details.getKey())).collect(Collectors.toSet());


        /**
         * All these are request resolved condition keys,
         * though specified required as a property instead of a smithy trait
         */
        return incorretRequestResolved.stream().map(
                record ->
                        error(record.shape,
                                "Use the @required trait on the field directly. "
                                        + "The required property is only for Service Resolved Condition Keys."
                        )).collect(Collectors.toList());
    }
}

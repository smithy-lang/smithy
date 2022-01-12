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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SetUtils;

/**
 * Finds operations that are meant to partially update a resource, but that use
 * members with default values in their input shapes, making it impossible
 * to know if the member was provided explicitly or defaulted.
 */
public final class DefaultValueInUpdateValidator extends AbstractValidator {

    // Look for operation names that start with update or patch.
    private static final Set<String> OPERATION_NAMES_TO_CHECK = SetUtils.of("update", "patch");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<String> defaultedMembers = new TreeSet<>();

        for (OperationShape operation : identifyOperations(model)) {
            StructureShape input = model.expectShape(operation.getInputShape(), StructureShape.class);

            for (MemberShape member : input.getAllMembers().values()) {
                if (member.hasTrait(DefaultTrait.class)) {
                    defaultedMembers.add(member.getMemberName());
                }
            }

            if (!defaultedMembers.isEmpty()) {
                events.add(warning(operation, "This update style operation has top-level input members marked with "
                                              + "the @default trait. It will be impossible to tell if the member was "
                                              + "omitted or explicitly provided. Affected members: "
                                              + defaultedMembers));
                defaultedMembers.clear();
            }
        }

        return events;
    }

    private Set<OperationShape> identifyOperations(Model model) {
        Set<OperationShape> operationsToCheck = new HashSet<>();

        // Look for operations that have an update lifecycle.
        for (ResourceShape resource : model.getResourceShapes()) {
            resource.getUpdate().ifPresent(id -> operationsToCheck.add(model.expectShape(id, OperationShape.class)));
        }

        // Look for operations based on their names.
        for (OperationShape operation : model.getOperationShapes()) {
            String name = operation.getId().getName().toLowerCase(Locale.ENGLISH);
            for (String prefix : OPERATION_NAMES_TO_CHECK) {
                if (name.startsWith(prefix)) {
                    operationsToCheck.add(operation);
                    break;
                }
            }
        }

        return operationsToCheck;
    }
}

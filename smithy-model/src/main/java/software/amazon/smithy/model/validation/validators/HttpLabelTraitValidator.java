/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that httpLabel traits are applied correctly for operation inputs.
 *
 * <ul>
 *     <li>Validates that if an operation has labels then it must have
 *     input.</li>
 *     <li>Validates that a corresponding input member can be found for each
 *     label in each operation.</li>
 *     <li>Validates that the correct target type is used for greedy and
 *     non-greedy labels.</li>
 *     <li>Validates that all labels in the URI of each operation that
 *     references the structure, have a corresponding member with the
 *     httpLabel trait.</li>
 * </ul>
 */
public final class HttpLabelTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        // Validate all operation shapes with the `http` trait.
        for (OperationShape operation : model.getOperationShapesWithTrait(HttpTrait.class)) {
            events.addAll(validateStructure(model, operation, operation.expectTrait(HttpTrait.class)));
        }

        return events;
    }

    private List<ValidationEvent> validateStructure(Model model, OperationShape operation, HttpTrait http) {
        return validateBindings(model,
                operation,
                http,
                model.expectShape(operation.getInputShape(), StructureShape.class));
    }

    private List<ValidationEvent> validateBindings(
            Model model,
            OperationShape operation,
            HttpTrait http,
            StructureShape input
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        // Create a set of labels and remove from the set when a match is
        // found. If any labels remain after looking at all members, then
        // there are unmatched labels.
        Set<String> labels = http.getUri()
                .getLabels()
                .stream()
                .map(UriPattern.Segment::getContent)
                .collect(Collectors.toSet());

        for (MemberShape member : input.getAllMembers().values()) {
            member.getTrait(HttpLabelTrait.class).ifPresent(trait -> {
                labels.remove(member.getMemberName());

                // Emit an error if the member is not a valid label.
                if (!http.getUri().getLabel(member.getMemberName()).isPresent()) {
                    events.add(error(member,
                            trait,
                            format(
                                    "This `%s` structure member is marked with the `httpLabel` trait, but no "
                                            + "corresponding `http` URI label could be found when used as the input of "
                                            + "the `%s` operation.",
                                    member.getMemberName(),
                                    operation.getId())));
                } else if (http.getUri().getLabel(member.getMemberName()).get().isGreedyLabel()) {
                    model.getShape(member.getTarget()).ifPresent(target -> {
                        // Greedy labels must be strings.
                        if (!target.isStringShape()) {
                            events.add(error(member,
                                    trait,
                                    format(
                                            "The `%s` structure member corresponds to a greedy label when used as the "
                                                    + "input of the `%s` operation. This member targets %s, but greedy labels "
                                                    + "must target string shapes.",
                                            member.getMemberName(),
                                            operation.getId(),
                                            target)));
                        }
                    });
                }
            });
        }

        if (!labels.isEmpty()) {
            events.add(error(operation,
                    String.format(
                            "This operation uses `%s` as input, but the following URI labels found in the operation's "
                                    + "`http` trait do not have a corresponding member marked with the `httpLabel` trait: %s",
                            input.getId(),
                            ValidationUtils.tickedList(labels))));
        }

        return events;
    }
}

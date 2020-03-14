/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.pattern.SmithyPattern;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that hostLabel traits are applied correctly for operation inputs.
 *
 * <ul>
 *     <li>Validates that if an operation's endpoint hostPrefix has labels
 *     then it must have input.</li>
 *     <li>Validates that a corresponding top-level input member can be found
 *     for each label in each operation endpoint hostPrefix.</li>
 *     <li>Validates that the correct type target type and traits are used for
 *     labels.</li>
 *     <li>Validates that all labels in the endpoint hostPrefix of each
 *     operation have corresponding hostLabel traits.</li>
 *     <li>Validates that an expanded version of each endpoint hostPrefix of
 *     each operation is a valid RFC 3896 host.</li>
 * </ul>
 */
public class HostLabelTraitValidator extends AbstractValidator {
    /**
     * Match the expanded template to be a valid RFC 3896 host.
     */
    private static final java.util.regex.Pattern EXPANDED_PATTERN = java.util.regex.Pattern.compile(
            "^[a-zA-Z0-9][a-zA-Z0-9-]{0,62}(?>\\.[a-zA-Z0-9-]{1,63})*\\.?$");

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Validate all operation shapes with the `endpoint` trait.
        return model.shapes(OperationShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, EndpointTrait.class))
                .flatMap(pair -> validateStructure(model, pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateStructure(
            Model model,
            OperationShape operation,
            EndpointTrait endpoint
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        // Validate the host can become a valid RFC 3986 Section 3.2.2 host.
        validateExpandedPattern(operation, endpoint).ifPresent(events::add);

        // If the operation has labels then it must also have input.
        if (!operation.getInput().isPresent() && !endpoint.getHostPrefix().getLabels().isEmpty()) {
            events.add(error(operation, endpoint, format(
                    "`endpoint` trait hostPrefix contains labels (%s), but operation has no input.",
                    ValidationUtils.tickedList(endpoint.getHostPrefix().getLabels().stream()
                            .map(SmithyPattern.Segment::getContent).collect(Collectors.toSet())))));
        } else {
            // Only validate the bindings if the input is a structure. Typing
            // validation of the input is handled elsewhere.
            operation.getInput()
                    .flatMap(model::getShape)
                    .flatMap(Shape::asStructureShape)
                    .ifPresent(input -> events.addAll(validateBindings(operation, endpoint, input)));
        }

        return events;
    }

    private List<ValidationEvent> validateBindings(
            OperationShape operation,
            EndpointTrait endpoint,
            StructureShape input
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        SmithyPattern hostPrefix = endpoint.getHostPrefix();

        // Create a set of labels and remove from the set when a match is
        // found. If any labels remain after looking at all members, then
        // there are unmatched labels.
        Set<String> labels = hostPrefix.getLabels().stream()
                .map(SmithyPattern.Segment::getContent)
                .collect(Collectors.toSet());

        input.getAllMembers().values().stream()
                .flatMap(member -> Trait.flatMapStream(member, HostLabelTrait.class))
                .forEach(pair -> {
                    MemberShape member = pair.getLeft();
                    HostLabelTrait trait = pair.getRight();
                    labels.remove(member.getMemberName());
                    if (!hostPrefix.getLabel(member.getMemberName()).isPresent()) {
                        events.add(error(member, trait, format(
                                "This `%s` structure member is marked with the `hostLabel` trait, but no "
                                + "corresponding `endpoint` label could be found when used as the input of "
                                + "the `%s` operation.", member.getMemberName(), operation.getId())));
                    }
                });

        if (!labels.isEmpty()) {
            events.add(error(input, format(
                    "This structure is used as the input for the `%s` operation, but the following host labels "
                    + "found in the operation's `endpoint` trait do not have a corresponding member marked with the "
                    + "`hostLabel` trait: %s", operation.getId(), ValidationUtils.tickedList(labels))));
        }

        return events;
    }

    private Optional<ValidationEvent> validateExpandedPattern(
            OperationShape operation,
            EndpointTrait endpoint
    ) {
        // Replace all label portions with stubs so the hostPrefix
        // can be validated.
        String stubHostPrefix = endpoint.getHostPrefix().getSegments().stream()
                .map(segment -> segment.isLabel() ? "foo" : segment.getContent())
                .collect(Collectors.joining());
        if (!EXPANDED_PATTERN.matcher(stubHostPrefix).matches()) {
            return Optional.of(error(operation, endpoint, format("The `endpoint` trait hostPrefix, %s, could "
                    + "not expand in to a valid RFC 3986 host: %s", endpoint.getHostPrefix(), stubHostPrefix)));
        }
        return Optional.empty();
    }
}

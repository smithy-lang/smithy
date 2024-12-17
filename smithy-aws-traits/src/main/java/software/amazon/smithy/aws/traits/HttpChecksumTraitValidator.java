/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates the HttpChecksum trait.
 *
 * <ul>
 *     <li>Validates that at least one of the request or response checksum
 *     behavior is configured.</li>
 *     <li>Validates that the {@code requestAlgorithmMember} and
 *     {@code requestChecksumModeMember} properties point to valid input members.</li>
 *     <li>Validates that the {@code requestAlgorithmMember} targeted member contains
 *     only supported algorithms.</li>
 *     <li>Validates that the {@code requestChecksumModeMember} targeted member contains
 *     only supported modes.</li>
 *     <li>Checks for conflicts with {@code @httpHeader} and {@code @httpPrefixHeaders}.</li>
 * </ul>
 */
@SmithyInternalApi
public final class HttpChecksumTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (OperationShape operation : model.getOperationShapesWithTrait(HttpChecksumTrait.class)) {
            events.addAll(validateOperation(model, operation));
        }

        return events;
    }

    private List<ValidationEvent> validateOperation(Model model, OperationShape operation) {
        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);

        // Request checksum behavior is defined when either the `requestChecksumRequired`
        // property or `requestAlgorithmMember` property are modeled.
        boolean isRequestChecksumConfiguration =
                trait.isRequestChecksumRequired() || trait.getRequestAlgorithmMember().isPresent();

        // Response checksum behavior is defined when either the `requestValidationModeMember`
        // property or `responseAlgorithms` property are modeled. Actually both are needed, but this check helps do
        // deeper validation later.
        boolean isResponseChecksumConfiguration =
                !trait.getResponseAlgorithms().isEmpty() || trait.getRequestValidationModeMember().isPresent();

        if (!isRequestChecksumConfiguration && !isResponseChecksumConfiguration) {
            return ListUtils.of(error(operation,
                    trait,
                    "The `httpChecksum` trait must define at least one of the"
                            + " `request` or `response` checksum behaviors."));
        }

        List<ValidationEvent> events = new ArrayList<>();

        // TraitTarget validation will raise an error if there's no operation input.
        model.getShape(operation.getInputShape()).flatMap(Shape::asStructureShape).ifPresent(input -> {
            if (isRequestChecksumConfiguration) {
                events.addAll(validateRequestChecksumConfiguration(model, trait, operation, input));
            }

            if (isResponseChecksumConfiguration) {
                events.addAll(validateResponseChecksumConfiguration(model, trait, operation, input));
            }
        });

        return events;
    }

    private List<ValidationEvent> validateRequestChecksumConfiguration(
            Model model,
            HttpChecksumTrait trait,
            OperationShape operation,
            StructureShape input
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        // Validate the requestAlgorithmMember is set properly for request behavior.
        validateAlgorithmMember(model, trait, operation, input).ifPresent(events::add);

        // Check for header binding conflicts with the input shape.
        events.addAll(validateHeaderConflicts(operation, input));

        return events;
    }

    private Optional<ValidationEvent> validateAlgorithmMember(
            Model model,
            HttpChecksumTrait trait,
            OperationShape operation,
            StructureShape input
    ) {
        // Validate that requestAlgorithmMember, if present, targets a properly configured member.
        if (trait.getRequestAlgorithmMember().isPresent()) {
            return validateEnumMember(model,
                    trait,
                    HttpChecksumTrait.REQUEST_ALGORITHM_MEMBER,
                    operation,
                    input,
                    trait.getRequestAlgorithmMember().get(),
                    HttpChecksumTrait.CHECKSUM_ALGORITHMS);
        }
        return Optional.empty();
    }

    @SuppressWarnings("deprecation")
    private Optional<ValidationEvent> validateEnumMember(
            Model model,
            HttpChecksumTrait trait,
            String traitProperty,
            OperationShape operation,
            StructureShape input,
            String memberName,
            List<String> supportedValues
    ) {
        Optional<MemberShape> member = input.getMember(memberName);
        // There's no member that matches the configured name.
        if (!member.isPresent()) {
            return Optional.of(error(operation,
                    trait,
                    format("The `%s` property of the `httpChecksum` trait targets"
                            + " a member that does not exist.", traitProperty)));
        }

        // Validate the enum contains only supported values.
        Optional<EnumTrait> enumTraitOptional = member.get().getMemberTrait(model, EnumTrait.class);
        List<String> unsupportedValues = new ArrayList<>();
        if (enumTraitOptional.isPresent()) {
            for (String value : enumTraitOptional.get().getEnumDefinitionValues()) {
                if (!supportedValues.contains(value)) {
                    unsupportedValues.add(value);
                }
            }
        } else {
            // This member does not have an enum trait.
            return Optional.of(error(operation,
                    trait,
                    format("The `%s` property of the `httpChecksum` trait targets"
                            + " a member that does not resolve an `enum` trait.", traitProperty)));
        }

        // Valid enum target containing only supported values.
        if (unsupportedValues.isEmpty()) {
            return Optional.empty();
        }

        // The member has an enum that contains unsupported values.
        return Optional.of(error(operation,
                trait,
                format("The `%s` property of the `httpChecksum` trait targets"
                        + " a member with an `enum` trait that contains unsupported values: %s",
                        traitProperty,
                        ValidationUtils.tickedList(unsupportedValues))));
    }

    private List<ValidationEvent> validateHeaderConflicts(OperationShape operation, StructureShape containerShape) {
        List<ValidationEvent> events = new ArrayList<>();

        for (MemberShape member : containerShape.members()) {
            // Emit a DANGER event if the prefix used with the HttpChecksum trait conflicts with any member
            // with HttpPrefixHeaders trait within the container shape. This is a DANGER event as members modeled
            // with HttpPrefixHeaders may unintentionally conflict with the checksum header resolved using the
            // HttpChecksum trait.
            member.getTrait(HttpPrefixHeadersTrait.class)
                    .map(HttpPrefixHeadersTrait::getValue)
                    .ifPresent(headerPrefix -> {
                        if (HttpChecksumTrait.CHECKSUM_PREFIX.startsWith(headerPrefix)) {
                            String memberName = member.getId().getName();
                            String prefixString = headerPrefix.toLowerCase(Locale.US);
                            events.add(danger(operation,
                                    format("The `httpPrefixHeaders` binding of `%s` uses"
                                            + " the prefix `%s` that conflicts with the prefix `%s` used by the"
                                            + " `httpChecksum` trait.",
                                            memberName,
                                            prefixString,
                                            HttpChecksumTrait.CHECKSUM_PREFIX),
                                    "HttpPrefixHeaders",
                                    memberName,
                                    prefixString));
                        }
                    });

            // Service may model members that are bound to the http header for checksum on input or output shape.
            // This enables customers to provide checksum as input, or access returned checksum value in output.
            // We trigger a WARNING event to prevent any unintentional behavior.
            member.getTrait(HttpHeaderTrait.class)
                    .map(HttpHeaderTrait::getValue)
                    .ifPresent(headerName -> {
                        if (headerName.startsWith(HttpChecksumTrait.CHECKSUM_PREFIX)) {
                            String memberName = member.getId().getName();
                            String headerString = headerName.toLowerCase(Locale.US);
                            events.add(warning(operation,
                                    format("The `httpHeader` binding of `%s` on `%s`"
                                            + " starts with the prefix `%s` used by the `httpChecksum` trait.",
                                            headerString,
                                            memberName,
                                            HttpChecksumTrait.CHECKSUM_PREFIX),
                                    "HttpHeader",
                                    memberName,
                                    headerString));
                        }
                    });
        }
        return events;
    }

    private List<ValidationEvent> validateResponseChecksumConfiguration(
            Model model,
            HttpChecksumTrait trait,
            OperationShape operation,
            StructureShape input
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        // Validate requestValidationModeMember is set properly for response behavior.
        if (!trait.getRequestValidationModeMember().isPresent()) {
            events.add(error(operation,
                    trait,
                    "The `httpChecksum` trait must model the"
                            + " `requestValidationModeMember` property to support response checksum behavior."));
        } else {
            validateValidationModeMember(model, trait, operation, input).map(events::add);
        }

        // Validate responseAlgorithms is not empty.
        if (trait.getResponseAlgorithms().isEmpty()) {
            events.add(error(operation,
                    trait,
                    "The `httpChecksum` trait must model the"
                            + " `responseAlgorithms` property to support response checksum behavior."));
        }

        // Check for header binding conflicts with the output shape.
        model.getShape(operation.getOutputShape()).flatMap(Shape::asStructureShape).ifPresent(outputShape -> {
            events.addAll(validateHeaderConflicts(operation, outputShape));
        });

        // Check for header binding conflicts with each error shape.
        if (!operation.getErrors().isEmpty()) {
            for (ShapeId id : operation.getErrors()) {
                StructureShape shape = model.expectShape(id, StructureShape.class);
                events.addAll(validateHeaderConflicts(operation, shape));
            }
        }

        return events;
    }

    private Optional<ValidationEvent> validateValidationModeMember(
            Model model,
            HttpChecksumTrait trait,
            OperationShape operation,
            StructureShape input
    ) {
        // Validate that requestValidationModeMember, which we've found already, targets a properly configured member.
        return validateEnumMember(model,
                trait,
                HttpChecksumTrait.REQUEST_VALIDATION_MODE_MEMBER,
                operation,
                input,
                trait.getRequestValidationModeMember().get(),
                HttpChecksumTrait.VALIDATION_MODES);
    }
}

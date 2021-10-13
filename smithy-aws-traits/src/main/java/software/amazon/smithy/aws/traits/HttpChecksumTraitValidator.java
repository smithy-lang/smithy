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

package software.amazon.smithy.aws.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates the HttpChecksum trait is modeled with at least one of the
 * request or response checksum behavior. Validates trait properties
 * `requestAlgorithmMember` and `requestChecksumModeMember` point to
 * valid input members. Also validates for possible conflict with
 * `httpHeader` and `httpPrefixHeaders` bindings within the same
 * operation input, output, or error structure.
 */
@SmithyInternalApi
public final class HttpChecksumTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        List<ServiceShape> services = model.shapes(ServiceShape.class).collect(Collectors.toList());
        for (ServiceShape service : services) {
            for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
                if (operation.hasTrait(HttpChecksumTrait.class)) {
                    events.addAll(validateOperation(model, service, operation));
                }
            }
        }
        return events;
    }

    /**
     * Validates an operation modeled with the `httpChecksum` trait.
     *
     * @param model     Model to validate.
     * @param service   Service the operation is bound within.
     * @param operation Operation modeled with `httpChecksum` trait.
     * @return Returns list of triggered validation events.
     */
    private List<ValidationEvent> validateOperation(Model model, ServiceShape service, OperationShape operation) {
        List<ValidationEvent> events = new ArrayList<>();
        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);

        // Modeled trait supports request checksum behavior when at least one of `requestChecksumRequired`
        // property or `requestAlgorithmMember` property is modeled.
        boolean isRequestChecksumConfiguration =
                trait.isRequestChecksumRequired() || trait.getRequestAlgorithmMember().isPresent();

        // Modeled trait supports response checksum behavior when both `requestValidationModeMember`
        // property or `responseAlgorithms` property are modeled.
        boolean isResponseChecksumConfiguration =
                !trait.getResponseAlgorithms().isEmpty() || trait.getRequestValidationModeMember().isPresent();

        if (!isRequestChecksumConfiguration && !isResponseChecksumConfiguration) {
            events.add(error(operation, trait, "The `httpChecksum` trait must define at least one of the"
                    + " `request` or `response` checksum behavior"));
            return events;
        }

        // validate httpChecksum trait models valid input member names.
        if (!operation.getInput().isPresent()) {
            events.add(error(operation, trait,
                    String.format("Operations modeled with `httpChecksum` trait MUST have a modeled input,"
                            + " `%s` does not.", operation.getId().getName(service))));
            return events;
        }
        events.addAll(validateInputMember(model, service, operation));

        if (isRequestChecksumConfiguration) {
            // validate conflicts for header binding for input shape.
            StructureShape inputShape = model.expectShape(operation.getInput().get(), StructureShape.class);
            events.addAll(validateHeader(service, operation, inputShape));
        }

        if (isResponseChecksumConfiguration) {
            // validate response checksum behavior modeled on the trait.
            events.addAll(validateResponseChecksumConfiguration(model, service, operation));

            // validate conflicts for header bindings for error shape.
            if (!operation.getErrors().isEmpty()) {
                for (ShapeId id : operation.getErrors()) {
                    StructureShape shape = model.expectShape(id, StructureShape.class);
                    events.addAll(validateHeader(service, operation, shape));
                }
            }

            // validate conflicts for header bindings for output shape.
            if (!operation.getOutput().isPresent()) {
                events.add(error(operation, trait,
                        String.format("Operations modeled with `httpChecksum` trait MUST have a modeled output,"
                                + " `%s` does not.", operation.getId().getName(service))));
                return events;
            }
            StructureShape outputShape = model.expectShape(operation.getOutput().get(), StructureShape.class);
            events.addAll(validateHeader(service, operation, outputShape));
        }
        return events;
    }

    /**
     * Validates the prefix `x-amz-checksum-` used with the httpChecksum trait does not conflict with header names
     * modeled using the `httpPrefixHeaders` trait or `httpHeader` trait.
     *
     * @param service        Service shape for the API.
     * @param operation      Operation shape on which httpChecksum trait is modeled.
     * @param containerShape Structure shape referenced by the operation shape, this can be either input, output,
     *                       or error shape.
     * @return Returns list of triggered validation events.
     */
    private List<ValidationEvent> validateHeader(
            ServiceShape service,
            OperationShape operation,
            StructureShape containerShape
    ) {
        String prefix = checksumHeaderPrefix().get();
        List<ValidationEvent> events = new ArrayList<>();

        for (MemberShape member : containerShape.members()) {
            // Trigger a DANGER event if the prefix used with the HttpChecksum trait conflicts with any member
            // with HttpPrefixHeaders trait within the container shape. This is a DANGER event as member modeled
            // with HttpPrefixHeaders may unintentionally conflict with the checksum header resolved using
            // HttpChecksum trait.
            member.getTrait(HttpPrefixHeadersTrait.class).ifPresent(httpPrefixHeadersTrait -> {
                String headerPrefix = httpPrefixHeadersTrait.getValue();
                if (prefix.startsWith(headerPrefix)) {
                    events.add(danger(operation, String.format("The `httpPrefixHeaders` binding of `%s` uses prefix"
                                    + " `%s` that conflicts prefix `%s` used by the `httpChecksum` trait.",
                            member.getId().getName(service), headerPrefix, prefix)));
                }
            });

            // Service may model members that are bound to the http header for checksum on input or output shape.
            // This enables customers to provide checksum as input, or access returned checksum value in output.
            // We trigger a WARNING event to prevent any unintentional behavior.
            member.getTrait(HttpHeaderTrait.class).ifPresent(headerTrait -> {
                String headerName = headerTrait.getValue();
                if (headerName.startsWith(prefix)) {
                    events.add(warning(operation, String.format("The `httpHeader` binding of `%s` on `%s` starts with"
                                    + " the prefix `%s` used by the `httpChecksum` trait.",
                            headerName, member.getId().getName(service), prefix)));
                }
            });
        }
        return events;
    }


    /**
     * Validates the `requestAlgorithmMember` and `requestValidationModeMember` property on
     * the httpChecksum trait.
     * <p>
     * Both the `requestAlgorithmMember` and `requestValidationModeMember` must be modeled with the name of an
     * input member targeting an Enum shape.
     *
     * @param model           The generation model.
     * @param service         Service shape for the API.
     * @param operation       Operation shape on which httpChecksum trait is modeled.
     * @return List of validation events that occurred when validating the model.
     */
    private List<ValidationEvent> validateInputMember(
            Model model,
            ServiceShape service,
            OperationShape operation
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        Map<String, String> modeledTraitMembers = new HashMap<String, String>();
        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);
        StructureShape inputShape = model.expectShape(operation.getInput().get(), StructureShape.class);
        Map<String, MemberShape> inputMembers = inputShape.getAllMembers();

        Optional<String> requestAlgorithmMember = trait.getRequestAlgorithmMember();
        if (requestAlgorithmMember.isPresent()) {
            modeledTraitMembers.put("requestAlgorithmMember", requestAlgorithmMember.get());
        }

        Optional<String> requestValidationModeMember = trait.getRequestValidationModeMember();
        if (requestValidationModeMember.isPresent()) {
            modeledTraitMembers.put("requestValidationModeMember", requestValidationModeMember.get());
        }

        for (Map.Entry<String, String> entry : modeledTraitMembers.entrySet()) {
            String propertyName = entry.getKey();
            String memberName = entry.getValue();
            if (!inputMembers.containsKey(memberName)) {
                events.add(error(operation, trait,
                        String.format("For operation `%s`, expected name modeled with `%s` property must"
                                        + " correspond to an input member.",
                                operation.getId().getName(service), propertyName)));
                continue;
            }

            Shape targetShape = model.expectShape(inputMembers.get(memberName).getTarget());
            if (!targetShape.hasTrait(EnumTrait.class)) {
                events.add(error(operation, trait,
                        String.format("For operation `%s`, input member name `%s` modeled with `%s` property"
                                        + "  of httpChecksum trait must be an enum shape.",
                                operation.getId().getName(service), memberName, propertyName)));
            }
        }
        return events;
    }


    /**
     * @return Returns a supplier that supplies set of supported checksum algorithm to validate when
     * returned within the response.
     */
    protected Supplier<Set<String>> responseChecksumAlgorithmSupplier() {
        Set<String> set = new HashSet<String>();
        set.add("CRC32C");
        set.add("CRC32");
        set.add("SHA1");
        set.add("SHA256");
        return () -> set;
    }

    /**
     * @return Returns a supplier that supplies prefix string used as checksum header prefix
     * for request and response checksum headers.
     */
    protected Supplier<String> checksumHeaderPrefix() {
        return () -> "x-amz-checksum-";
    }

    /**
     * Validates response checksum behavior modeled with httpChecksum trait. Validates trait models both
     * `requestValidationModeMember` property and non-empty `responseAlgorithms` property.
     * Also validates modeled response algorithms are known checksum algorithms.
     *
     * @param model      The generation model.
     * @param service    Service shape for the API.
     * @param operation  Operation shape on which httpChecksum trait is modeled.
     * @return List of validation events that occurred when validating the model.
     */
    private List<ValidationEvent> validateResponseChecksumConfiguration(
            Model model,
            ServiceShape service,
            OperationShape operation
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);
        if (!trait.getRequestValidationModeMember().isPresent()) {
            events.add(error(operation, trait, String.format("For operation `%s`, the `httpChecksum` trait must model"
                                    + " `requestValidationModeMember` property to support response checksum behavior.",
                            operation.getId().getName(service))));
            return events;
        }

        List<String> algorithms = trait.getResponseAlgorithms();
        if (algorithms.isEmpty()) {
            events.add(error(operation, trait, String.format("For operation `%s`, the `httpChecksum` trait must model"
                            + " `responseAlgorithms` property with at least one algorithm to support response checksum"
                            + " behavior.", operation.getId().getName(service))));
            return events;
        }

        Set<String> validAlgorithms = responseChecksumAlgorithmSupplier().get();
        for (String algorithm : algorithms) {
            if (!validAlgorithms.contains(algorithm.toUpperCase())) {
                events.add(error(operation, trait,
                        String.format("For operation `%s`, httpChecksum trait models an unsupported response algorithm"
                                        + " `%s`. Supported algorithms are `%s`.",
                                operation.getId().getName(service), algorithm, validAlgorithms)));
            }
        }

        return events;
    }
}

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.protocols.AwsProtocolTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumProperty;
import software.amazon.smithy.model.traits.HttpChecksumProperty.Location;
import software.amazon.smithy.model.traits.HttpChecksumTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates checksum location modeling specific to AWS usage. If service, operation uses sigv4 authentication scheme,
 * the request property within httpChecksum trait must include "header" as supported checksum location. Validates only
 * supported checksum behavior is modeled for request or response for AWS use cases.
 */
@SmithyInternalApi
public class HttpChecksumTraitValidator extends AbstractValidator {

    /**
     * @return Returns a supplier for valid target protocols for which validation should be performed.
     */
    protected Supplier<List<Class<? extends Trait>>> targetProtocolSupplier() {
        return () -> ListUtils.of(AwsProtocolTrait.class);
    }

    /**
     * @return Returns a supplier that supplies set of supported checksum behavior for request.
     */
    protected Supplier<Set<HttpChecksumProperty>> supportedRequestChecksumSupplier() {
        HttpChecksumProperty.Builder builder = HttpChecksumProperty.builder();
        return () -> SetUtils.of(
                builder.algorithm("sha1").location("header").name("x-amz-checksum-sha1").build(),
                builder.algorithm("sha1").location("trailer").name("x-amz-checksum-sha1").build(),
                builder.algorithm("sha256").location("header").name("x-amz-checksum-sha256").build(),
                builder.algorithm("sha256").location("trailer").name("x-amz-checksum-sha256").build(),
                builder.algorithm("crc32").location("header").name("x-amz-checksum-crc32").build(),
                builder.algorithm("crc32").location("trailer").name("x-amz-checksum-crc32").build(),
                builder.algorithm("crc32c").location("header").name("x-amz-checksum-crc32c").build(),
                builder.algorithm("crc32c").location("trailer").name("x-amz-checksum-crc32c").build()
        );
    }

    /**
     * @return Returns a supplier that supplies set of supported checksum behavior for response.
     */
    protected Supplier<Set<HttpChecksumProperty>> supportedResponseChecksumSupplier() {
        HttpChecksumProperty.Builder builder = HttpChecksumProperty.builder();
        return () -> SetUtils.of(
                builder.algorithm("sha1").location("header").name("x-amz-checksum-sha1").build(),
                builder.algorithm("sha256").location("header").name("x-amz-checksum-sha256").build(),
                builder.algorithm("crc32").location("header").name("x-amz-checksum-crc32").build(),
                builder.algorithm("crc32c").location("header").name("x-amz-checksum-crc32c").build()
        );
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        List<ServiceShape> services = model.shapes(ServiceShape.class).collect(Collectors.toList());

        Supplier<List<Class<? extends Trait>>> protocolSupplier = targetProtocolSupplier();
        if (protocolSupplier == null) {
            throw new ExpectationNotMetException("Expected non null supplier for the list of target protocols"
                    + " for validation, found null.", SourceLocation.NONE);
        }

        Supplier<Set<HttpChecksumProperty>> requestChecksumSupplier = supportedRequestChecksumSupplier();
        if (requestChecksumSupplier == null) {
            throw new ExpectationNotMetException("Expected non null supplier for supported request checksum behavior"
                    + " , found null.", SourceLocation.NONE);
        }

        Supplier<Set<HttpChecksumProperty>> responseChecksumSupplier = supportedResponseChecksumSupplier();
        if (responseChecksumSupplier == null) {
            throw new ExpectationNotMetException("Expected non null supplier for supported response checksum behavior"
                    + " , found null.", SourceLocation.NONE);
        }

        for (ServiceShape service : services) {
            if (!isTargetProtocol(protocolSupplier, service)) {
                continue;
            }

            for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
                if (operation.hasTrait(HttpChecksumTrait.class)) {
                    events.addAll(validateBehaviorIsSupported(operation, requestChecksumSupplier,
                            responseChecksumSupplier));
                    events.addAll(validateRequestSupportsHeaderLocation(serviceIndex, service, operation));
                }
            }
        }

        return events;
    }

    /**
     * Validates checksum behavior modeled for request or response is supported for AWS usage.
     *
     * @param operation Operation shape to validate.
     * @param requestChecksumSupplier Supplier for set of supported request checksum behavior.
     * @param responseChecksumSupplier Supplier for set of supported response checksum behavior.
     * @return List of validation events that occurred when validating the model.
     */
    private List<ValidationEvent> validateBehaviorIsSupported(
            OperationShape operation,
            Supplier<Set<HttpChecksumProperty>> requestChecksumSupplier,
            Supplier<Set<HttpChecksumProperty>> responseChecksumSupplier
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);
        String formattedError = "The checksum behavior for %s with algorithm \"%s\", location \"%s\" and name"
                + " \"%s\" is not supported.";

        // validate if request properties are supported.
        Set<HttpChecksumProperty> validRequestChecksumBehavior = requestChecksumSupplier.get();
        for (HttpChecksumProperty property : trait.getRequestProperties()) {
            if (validRequestChecksumBehavior == null || !validRequestChecksumBehavior.contains(property)) {
                events.add(error(operation, trait,
                        String.format(formattedError, "request", property.getAlgorithm(), property.getLocation(),
                                property.getName())));
            }
        }

        // validate if response properties are supported.
        Set<HttpChecksumProperty> validResponseChecksumBehavior = responseChecksumSupplier.get();
        for (HttpChecksumProperty property : trait.getResponseProperties()) {
            if (validResponseChecksumBehavior == null || !validResponseChecksumBehavior.contains(property)) {
                events.add(error(operation, trait,
                        String.format(formattedError, "response", property.getAlgorithm(), property.getLocation(),
                                property.getName())));
            }
        }

        return events;
    }

    /**
     * Validates the request property includes "header" as supported checksum location,
     * when service, operation uses sigv4.
     *
     * @param serviceIndex index resolving auth schemes
     * @param service      service shape for the API
     * @param operation    operation shape
     * @return List of validation events that occurred when validating the model.
     */
    private List<ValidationEvent> validateRequestSupportsHeaderLocation(
            ServiceIndex serviceIndex,
            ServiceShape service,
            OperationShape operation
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);

        // if SigV4 auth scheme is used, validate request property locations contain "header" as supported location.
        if (hasSigV4AuthScheme(serviceIndex, service, operation)) {
            for (String algorithm : trait.getRequestAlgorithms()) {
                Boolean supportsHeaderAsLocation = trait.getRequestPropertiesForAlgorithm(algorithm)
                        .stream()
                        .anyMatch(p -> p.getLocation().equals(Location.HEADER));

                if (!supportsHeaderAsLocation) {
                    events.add(error(operation, trait,
                            String.format("Operations that support the `sigv4` trait MUST support the `header`"
                                    + " checksum location on `request`, \"%s\" does not for \"%s\" algorithm.",
                                    operation.getId().getName(service), algorithm)));
                }
            }
        }
        return events;
    }

    /**
     * isTargetProtocol returns true if service uses any target protocol supplied via the target protocol supplier.
     *
     * @param service is the service shape for which target protocol usage is checked.
     * @return boolean indicating target protocol is used by the service.
     */
    private boolean isTargetProtocol(Supplier<List<Class<? extends Trait>>> supplier, ServiceShape service) {
        List<Class<?extends  Trait>> list = supplier.get();
        if (list == null) {
            return false;
        }

        for (Class<? extends Trait> t : list) {
            if (service.hasTrait(t)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the SigV4Trait is a auth scheme for the service and operation.
     *
     * @param serviceIndex index resolving auth schemes
     * @param service      service shape for the API
     * @param operation    operation shape
     * @return if SigV4 is an auth scheme for the operation and service.
     */
    private boolean hasSigV4AuthScheme(ServiceIndex serviceIndex, ServiceShape service, OperationShape operation) {
        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service.getId(), operation.getId());
        return auth.containsKey(SigV4Trait.ID);
    }
}

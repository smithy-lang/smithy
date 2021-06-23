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
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.protocols.AwsProtocolTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumProperty;
import software.amazon.smithy.model.traits.HttpChecksumProperty.Location;
import software.amazon.smithy.model.traits.HttpChecksumTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates checksum location modeling specific to AWS usage. If service, operation uses sigv4 authentication scheme,
 * the request property within httpChecksum trait must include "header" as supported checksum location. Validates only
 * supported checksum behavior is modeled for request or response for AWS use cases.
 */
@SmithyInternalApi
public class HttpChecksumTraitValidator extends AbstractValidator {

    // Set of supported checksum behavior on request.
    private static final Set<String> SUPPORTED_BEHAVIOR_FOR_REQUEST = SetUtils.of(
            "sha1:header:x-amz-checksum-sha1",
            "sha1:trailer:x-amz-checksum-sha1",
            "sha256:header:x-amz-checksum-sha256",
            "sha256:trailer:x-amz-checksum-sha256",
            "crc32:header:x-amz-checksum-crc32",
            "crc32:trailer:x-amz-checksum-crc32",
            "crc32c:header:x-amz-checksum-crc32c",
            "crc32c:trailer:x-amz-checksum-crc32c"
    );

    // Set of supported checksum behavior on response.
    private static final Set<String> SUPPORTED_BEHAVIOR_FOR_RESPONSE = SetUtils.of(
            "sha1:header:x-amz-checksum-sha1",
            "sha256:header:x-amz-checksum-sha256",
            "crc32:header:x-amz-checksum-crc32",
            "crc32c:header:x-amz-checksum-crc32c"
    );


    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        List<ServiceShape> services = model.shapes(ServiceShape.class).collect(Collectors.toList());
        for (ServiceShape service : services) {
            if (!isTargetProtocol(service)) {
                continue;
            }

            for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
                if (operation.hasTrait(HttpChecksumTrait.class)) {
                    events.addAll(validateBehaviorIsSupported(operation));
                    events.addAll(validateRequestSupportsHeaderLocation(serviceIndex, service, operation));
                }
            }
        }

        return events;
    }

    /**
     * Validates checksum behavior modeled for request or response is supported for AWS usage.
     *
     * @param operation operation shape
     * @return List of validation events that occurred when validating the model.
     */
    protected List<ValidationEvent> validateBehaviorIsSupported(
            OperationShape operation
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);
        String formattedError = "The checksum behavior for %s with algorithm \"%s\", location \"%s\" and name"
                + " \"%s\" is not supported.";

        // validate if request properties are supported.
        for (HttpChecksumProperty value : trait.getRequestProperties()) {
            String str = value.getAlgorithm() + ":" + value.getLocation().toString() + ":" + value.getName();

            if (!SUPPORTED_BEHAVIOR_FOR_REQUEST.contains(str)) {
                events.add(error(operation, trait,
                        String.format(formattedError, "request", value.getAlgorithm(), value.getLocation(),
                                value.getName())));
            }
        }

        // validate if response properties are supported.
        for (HttpChecksumProperty value : trait.getResponseProperties()) {
            String str = value.getAlgorithm() + ":" + value.getLocation().toString() + ":" + value.getName();
            if (!SUPPORTED_BEHAVIOR_FOR_RESPONSE.contains(str)) {
                events.add(error(operation, trait,
                        String.format(formattedError, "response", value.getAlgorithm(), value.getLocation(),
                                value.getName())));
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
    protected List<ValidationEvent> validateRequestSupportsHeaderLocation(
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
                            String.format("For operation using sigv4 auth scheme, the `request` property of the"
                                            + " `httpChecksum` trait must support `header` checksum location."
                                            + " The \"%s\" algorithm does not support `header` location for request.",
                                    algorithm)));
                }
            }
        }
        return events;
    }

    /**
     * isTargetProtocol returns true if service uses a target protocol. By default,
     * target protocol resolves to aws protocol.
     *
     * @param service is the service shape for which target protocol usage is checked.
     * @return boolean indicating target protocol is used by the service.
     */
    protected boolean isTargetProtocol(ServiceShape service) {
        // By default, target protocol is AWS protocol.
        return service.hasTrait(AwsProtocolTrait.class);
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
        return auth.containsKey(SigV4Trait.ID) && !operation.hasTrait(OptionalAuthTrait.class);
    }
}

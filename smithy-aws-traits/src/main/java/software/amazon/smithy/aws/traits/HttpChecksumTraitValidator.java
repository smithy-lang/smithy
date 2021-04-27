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
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.protocols.AwsProtocolTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumProperties.Location;
import software.amazon.smithy.model.traits.HttpChecksumTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates checksum location modeling specific to AWS usage. For response property within httpChecksum trait,
 * only "header" is a valid checksum location. If service, operation uses sigv4 authentication scheme, the
 * request property within httpChecksum trait must include "header" as supported checksum location.
 */
@SmithyInternalApi
public class HttpChecksumTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        ServiceIndex serviceIndex = ServiceIndex.of(model);

        model.shapes(ServiceShape.class).filter(this::isTargetProtocol).forEach(service -> {
            TopDownIndex.of(model).getContainedOperations(service).forEach(operation -> {
                if (operation.hasTrait(HttpChecksumTrait.class)) {
                    events.addAll(validateSupportedLocations(serviceIndex, service, operation));
                }
            });
        });
        return events;
    }

    /**
     * Validates supported locations within httpChecksum trait. For response property, only "header"
     * is a valid checksum location. For service, operation using sigv4, the request property must include
     * "header" as supported checksum location.
     *
     * @param serviceIndex index resolving auth schemes
     * @param service      service shape for the API
     * @param operation    operation shape
     * @return List of validation events that occurred when validating the model.
     */
    protected List<ValidationEvent> validateSupportedLocations(
            ServiceIndex serviceIndex,
            ServiceShape service,
            OperationShape operation
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);

        // validate response property only supports "header" as location
        trait.getResponseProperty().ifPresent(property -> {
            Set<Location> locations = property.getLocations();
            if (locations.size() > 1 || !locations.contains(Location.HEADER)) {
                events.add(error(operation, trait,
                        String.format("For aws protocols, the `response` property of the `httpChecksum` trait "
                                + "only supports `header` as `location`, found \"%s\".", locations)));
            }
        });

        // if SigV4 auth scheme is used, validate request property locations contain "header" as supported location.
        if (hasSigV4AuthScheme(serviceIndex, service, operation)) {
            trait.getRequestProperty().ifPresent(property -> {
                if (!property.getLocations().contains(Location.HEADER)) {
                    events.add(error(operation, trait,
                            "For operation using sigv4 auth scheme, the `request` property of the "
                                    + "`httpChecksum` trait must support `header` checksum location."));
                }
            });
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

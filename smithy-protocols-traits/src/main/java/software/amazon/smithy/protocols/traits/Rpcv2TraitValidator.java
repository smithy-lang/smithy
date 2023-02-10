/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except
 * in compliance with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package software.amazon.smithy.protocols.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates models implementing the {@code Rpcv2Trait} against its constraints by:
 *
 * - Ensuring that every entry in {@code eventStreamHttp} also appears in the {@code http} property of
 * a protocol trait.
 * - Ensuring that there is at least one value for the format property.
 * - Ensuring all the format property values are valid and supported.
 */
@SmithyInternalApi
public final class Rpcv2TraitValidator extends AbstractValidator {

    private static final List<String> VALID_FORMATS = ListUtils.of("cbor");

    @Override
    public List<ValidationEvent> validate(Model model) {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        return model.shapes(ServiceShape.class)
                .flatMap(service -> validateService(service, serviceIndex).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(ServiceShape service, ServiceIndex index) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Trait protocol : index.getProtocols(service).values()) {
            if (protocol instanceof Rpcv2Trait) {
                Rpcv2Trait protocolTrait = (Rpcv2Trait) protocol;
                List<String> invalid = new ArrayList<>(protocolTrait.getEventStreamHttp());
                invalid.removeAll(protocolTrait.getHttp());
                if (!invalid.isEmpty()) {
                    events.add(error(service, protocol, String
                            .format("The following values of the `eventStreamHttp` property do "
                                    + "not also appear in the `http` property of the %s protocol "
                                    + "trait: %s", protocol.toShapeId(), invalid)));
                }
                List<String> formats = new ArrayList<>(protocolTrait.getFormat());
                // Validate there is at least one element in the format property.
                if (formats.size() == 0) {
                    events.add(error(service, protocol, String.format(
                            "The `format` property for the %s protocol must have at least 1 element",
                            protocol.toShapeId())));
                }
                // All the user specified wire formats must be valid.
                formats.removeAll(VALID_FORMATS);
                if (!formats.isEmpty()) {
                    events.add(error(service, protocol, String.format(
                            "The following values of the `format` property for the %s protocol "
                                    + "are not supported: %s",
                            protocol.toShapeId(), formats)));;
                }
            }
        }

        return events;
    }
}

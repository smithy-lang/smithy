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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates models implementing the {@code Rpcv2Trait} against its constraints by:
 *
 * - Ensuring that every entry in {@code eventStreamHttp} also appears in the {@code http} property
 *   of a protocol trait.
 * - Ensuring that there is at least one value for the {@code format} property.
 * - Ensuring all the {@code format} property values are valid and supported.
 */
@SmithyInternalApi
public final class Rpcv2TraitValidator extends AbstractValidator {

    private static final List<String> VALID_FORMATS = ListUtils.of("cbor");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(Rpcv2Trait.class)) {
            events.addAll(validateService(serviceShape));
        }
        return events;
    }

    private List<ValidationEvent> validateService(ServiceShape service) {
        List<ValidationEvent> events = new ArrayList<>();

        Rpcv2Trait protocolTrait = service.expectTrait(Rpcv2Trait.class);

        List<String> invalid = new ArrayList<>(protocolTrait.getEventStreamHttp());
        invalid.removeAll(protocolTrait.getHttp());
        if (!invalid.isEmpty()) {
            events.add(error(service, protocolTrait,
                    String.format("The following values of the `eventStreamHttp` property do "
                            + "not also appear in the `http` property of the %s protocol "
                            + "trait: %s", protocolTrait.toShapeId(), invalid)));
        }

        // The trait model validates having 1 or more formats and that formats are lowercase.
        List<String> formats = new ArrayList<>(protocolTrait.getFormat());
        // All the user specified wire formats must be valid.
        formats.removeAll(VALID_FORMATS);
        if (!formats.isEmpty()) {
            events.add(error(service, protocolTrait,
                    String.format(
                            "The following values of the `format` property for the %s protocol "
                                    + "are not supported: %s",
                            protocolTrait.toShapeId(), formats)));;
        }

        return events;
    }
}

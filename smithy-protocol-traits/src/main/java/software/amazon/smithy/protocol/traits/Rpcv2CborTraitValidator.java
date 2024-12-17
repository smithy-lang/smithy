/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates models implementing the {@code Rpcv2CborTrait} against its constraints by:
 *
 * - Ensuring that every entry in {@code eventStreamHttp} also appears in the {@code http} property
 *   of a protocol trait.
 */
@SmithyInternalApi
public final class Rpcv2CborTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(Rpcv2CborTrait.class)) {
            Rpcv2CborTrait protocolTrait = serviceShape.expectTrait(Rpcv2CborTrait.class);

            List<String> invalid = new ArrayList<>(protocolTrait.getEventStreamHttp());
            invalid.removeAll(protocolTrait.getHttp());
            if (!invalid.isEmpty()) {
                events.add(error(serviceShape,
                        protocolTrait,
                        String.format("The following values of the `eventStreamHttp` property do "
                                + "not also appear in the `http` property of the %s protocol "
                                + "trait: %s", protocolTrait.toShapeId(), invalid)));
            }
        }
        return events;
    }
}

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

/**
 * Validates that every entry in {@code eventStreamHttp} also appears in the {@code http}
 * property of an {@link Rpcv2ProtocolTrait}.
 *
 * <p>Concrete subclasses supply the trait class to validate against, and this base class
 * handles the shared validation logic for both {@link Rpcv2CborTrait} and
 * {@link Rpcv2JsonTrait}.
 *
 * @param <T> A concrete subclass of {@link Rpcv2ProtocolTrait}.
 */
abstract class Rpcv2ProtocolTraitValidator<T extends Rpcv2ProtocolTrait> extends AbstractValidator {

    private final Class<T> traitClass;

    Rpcv2ProtocolTraitValidator(Class<T> traitClass) {
        this.traitClass = traitClass;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(traitClass)) {
            T protocolTrait = serviceShape.expectTrait(traitClass);
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

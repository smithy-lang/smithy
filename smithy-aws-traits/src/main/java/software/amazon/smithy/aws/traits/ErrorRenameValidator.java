/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryTrait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class ErrorRenameValidator extends AbstractValidator {

    /**
     * By default, server-side implementation of AWS Protocols send the error shape ID on the wire,
     * and clients can use it to resolve the error type.
     *
     * However, some protocols implementations, including AWS JSON 1.1, only send the shape name.
     * If there are conflicting shape IDs e.g. smithy.service#ServiceError and smithy.other#ServiceError,
     * clients are unable to resolve the error type with the shape name alone.
     *
     * In addition, Server-side implementation of these protocols don't handle or send renamed shape names.
     *
     * Hence, error shape renaming are not supported for these protocols.
     */
    private static final Set<ShapeId> UNSUPPORTED_PROTOCOLS = SetUtils.of(
            AwsJson1_0Trait.ID,
            AwsJson1_1Trait.ID,
            AwsQueryTrait.ID,
            Ec2QueryTrait.ID,
            RestJson1Trait.ID,
            RestXmlTrait.ID);

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape shape : model.getServiceShapes()) {
            validate(model, shape, events);
        }
        return events;
    }

    private void validate(Model model, ServiceShape service, List<ValidationEvent> events) {
        final Map<ShapeId, String> renames = service.getRename();

        if (renames.isEmpty()) {
            return;
        }

        Set<String> unsupportedProtocols = new HashSet<>();
        for (ShapeId protocol : UNSUPPORTED_PROTOCOLS) {
            if (service.getAllTraits().containsKey(protocol)) {
                unsupportedProtocols.add(protocol.getName());
            }
        }

        if (unsupportedProtocols.isEmpty()) {
            return;
        }

        renames.keySet().forEach(shapeId -> {
            Optional<Shape> shape = model.getShape(shapeId);

            if (!shape.isPresent() || !shape.get().hasTrait(ErrorTrait.class)) {
                return;
            }

            ShapeId from = shape.get().getId();
            String to = renames.get(from);
            events.add(error(service,
                    String.format(
                            "Service attempts to rename an error shape from `%s` to \"%s\"; "
                                    + "Service protocols %s do not support error renaming.",
                            from,
                            to,
                            unsupportedProtocols)));
        });
    }
}

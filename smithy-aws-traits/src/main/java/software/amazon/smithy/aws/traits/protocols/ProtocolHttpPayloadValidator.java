/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBinding.Location;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Ensures that the http payload trait is only bound to structures, unions,
 * documents, blobs, or strings for AWS protocols.
 */
@SmithyInternalApi
public final class ProtocolHttpPayloadValidator extends AbstractValidator {
    private static final Set<ShapeType> VALID_HTTP_PAYLOAD_TYPES = SetUtils.of(
            ShapeType.STRUCTURE,
            ShapeType.UNION,
            ShapeType.DOCUMENT,
            ShapeType.BLOB,
            ShapeType.STRING,
            ShapeType.ENUM);

    @Override
    public List<ValidationEvent> validate(Model model) {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        HttpBindingIndex bindingIndex = HttpBindingIndex.of(model);
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        return model.shapes(ServiceShape.class)
                .filter(service -> usesAwsProtocol(service, serviceIndex))
                .flatMap(service -> validateService(model, service, bindingIndex, topDownIndex).stream())
                .collect(Collectors.toList());
    }

    private boolean usesAwsProtocol(ServiceShape service, ServiceIndex index) {
        for (Trait protocol : index.getProtocols(service).values()) {
            if (protocol instanceof AwsProtocolTrait) {
                return true;
            }
        }
        return false;
    }

    private List<ValidationEvent> validateService(
            Model model,
            ServiceShape service,
            HttpBindingIndex bindingIndex,
            TopDownIndex topDownIndex
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            List<HttpBinding> requestBindings = bindingIndex.getRequestBindings(operation, Location.PAYLOAD);
            validateBindings(model, requestBindings).ifPresent(events::add);

            List<HttpBinding> responseBindings = bindingIndex.getResponseBindings(operation, Location.PAYLOAD);
            validateBindings(model, responseBindings).ifPresent(events::add);

            for (ShapeId error : operation.getErrors()) {
                List<HttpBinding> errorBindings = bindingIndex.getResponseBindings(error, Location.PAYLOAD);
                validateBindings(model, errorBindings).ifPresent(events::add);
            }
        }

        return events;
    }

    private Optional<ValidationEvent> validateBindings(Model model, Collection<HttpBinding> payloadBindings) {
        for (HttpBinding binding : payloadBindings) {
            if (!payloadBoundToValidType(model, binding.getMember().getTarget())) {
                return Optional.of(error(binding.getMember(),
                        "AWS Protocols only support binding the "
                                + "following shape types to the payload: string, blob, structure, union, and document"));
            }
        }
        return Optional.empty();
    }

    private boolean payloadBoundToValidType(Model model, ToShapeId payloadShape) {
        return model.getShape(payloadShape.toShapeId())
                .map(shape -> VALID_HTTP_PAYLOAD_TYPES.contains(shape.getType()))
                .orElse(false);
    }
}

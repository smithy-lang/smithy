/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Detects when a protocol indicates that it does not support inline documents,
 * yet the protocol trait is attached to a service that uses inline documents.
 */
public final class NoInlineDocumentSupportValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        Set<DocumentShape> documents = model.shapes(DocumentShape.class).collect(Collectors.toSet());
        if (documents.isEmpty()) {
            return Collections.emptyList();
        }

        Set<ShapeId> noInlineDocumentSupport = findProtocolsWithNoInlineDocumentSupport(model);
        if (noInlineDocumentSupport.isEmpty()) {
            return Collections.emptyList();
        }

        List<ServiceShape> services = model.shapes(ServiceShape.class).collect(Collectors.toList());
        Walker walker = new Walker(model);
        List<ValidationEvent> events = new ArrayList<>();

        for (ServiceShape service : services) {
            // Find all service shapes that use a protocol that does not
            // support documents.
            for (ShapeId protocol : noInlineDocumentSupport) {
                if (service.findTrait(protocol).isPresent()) {
                    // Find if the service uses a document.
                    Set<Shape> shapes = walker.walkShapes(service);
                    Set<Shape> foundDocuments = new TreeSet<>();
                    for (DocumentShape documentShape : documents) {
                        if (shapes.contains(documentShape)) {
                            foundDocuments.add(documentShape);
                        }
                    }
                    // Only emit if a document was found in the closure.
                    if (!foundDocuments.isEmpty()) {
                        events.add(createEvent(service, service.findTrait(protocol).get(), foundDocuments));
                    }
                }
            }
        }

        return events;
    }

    private Set<ShapeId> findProtocolsWithNoInlineDocumentSupport(Model model) {
        Set<ShapeId> noInlineDocumentSupport = new HashSet<>();
        for (Shape shape : model.getShapesWithTrait(ProtocolDefinitionTrait.class)) {
            ProtocolDefinitionTrait definitionTrait = shape.expectTrait(ProtocolDefinitionTrait.class);
            if (definitionTrait.getNoInlineDocumentSupport()) {
                noInlineDocumentSupport.add(shape.getId());
            }
        }
        return noInlineDocumentSupport;
    }

    private ValidationEvent createEvent(ServiceShape service, Trait protocol, Set<Shape> foundDocuments) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Shape document : foundDocuments) {
            joiner.add(document.getId() + " @ " + document.getSourceLocation());
        }

        return error(service,
                protocol,
                String.format(
                        "This service uses the `%s` protocol which does not support inline document types, "
                                + "but the following document types were found in the closure of the service: %s",
                        protocol.toShapeId(),
                        joiner.toString()));
    }
}

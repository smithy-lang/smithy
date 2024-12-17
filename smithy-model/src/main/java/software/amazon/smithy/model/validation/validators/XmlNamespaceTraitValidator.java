/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that the xmlNamespace traits are applied correctly for structures.
 *
 * <ul>
 *     <li>Validates that uri is valid.</li>
 * </ul>
 */
public final class XmlNamespaceTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(XmlNamespaceTrait.class)) {
            validateTrait(shape, shape.expectTrait(XmlNamespaceTrait.class)).ifPresent(events::add);
        }

        return events;
    }

    private Optional<ValidationEvent> validateTrait(Shape shape, XmlNamespaceTrait xmlNamespace) {
        // Validate the xmlNamespace URI against the URI specification.
        try {
            new java.net.URI(xmlNamespace.getUri());
        } catch (java.net.URISyntaxException uriSyntaxException) {
            return Optional.of(error(shape,
                    format(
                            "An `xmlNamespace` trait is applied to the %s shape with an invalid uri: %s",
                            shape,
                            xmlNamespace.getUri())));
        }

        return Optional.empty();
    }
}

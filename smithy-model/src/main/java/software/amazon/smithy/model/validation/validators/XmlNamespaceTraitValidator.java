/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
            return Optional.of(error(shape, format(
                    "An `xmlNamespace` trait is applied to the %s shape with an invalid uri: %s",
                    shape, xmlNamespace.getUri())));
        }

        return Optional.empty();
    }
}
